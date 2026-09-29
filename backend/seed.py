"""Demo data: the same queues, people and history the app shows from FakeData, in the database.

    python seed.py

**It empties every table first**, then fills them, so running it again always gives the same
clean demo (BACKEND.md step 2.4). Open queues are scheduled around the current time, like
FakeData does, so the demo makes sense whenever it's run.

Demo accounts (all use DEMO_PASSWORD):
    09175550001  Maria Santos   queuer, with a few past visits in history
    09175550002  Rhea Cruz      organizer, Brgy. Poblacion Council (verified), owns q1, q3, q5
    09180000000  Jun Dela Cruz  suspended ("Posting a fake queue")
    09990000000  Quapp Admin    can open the admin page
The other organizers and everyone standing in line are accounts too, so their tickets are real.
"""

from dataclasses import dataclass
from datetime import date, datetime, time, timedelta
from itertools import count

from sqlalchemy.orm import Session

from app import timeutil
from app.database import Base, SessionLocal, engine
from app.enums import Category, QueueStatus, TicketStatus, UserStatus, VerificationStatus
from app.models import Queue, Ticket, User, new_id
from app.security import hash_password

DEMO_PASSWORD = "quapp-demo"

FIRST_NAMES = ["Maria", "Jose", "Ana", "Pedro", "Rosa", "Carlos", "Elena", "Miguel"]
LAST_NAMES = ["Santos", "Rivera", "Lopez", "Cruz", "Mendoza", "Bautista", "Torres", "Reyes"]

# A queue's rolling average needs a few served people today; open queues that FakeData
# starts with fewer get this many, so their waits start near FakeData's instead of the default.
WARM_UP_SERVED = 5


@dataclass
class Line:
    """How a queue starts: people waiting, the wait to aim for, and today so far."""
    waiting: int = 0
    eta_minutes: int = 0
    served: int = 0
    no_shows: int = 0


def hours_from_now(hours: int) -> time:
    """Now in Manila, rounded down to the half hour, moved by some hours, kept within the day
    (FakeData.hoursFromNow), so a demo at 10 PM doesn't get a queue closing "at 3 AM"."""
    now = datetime.now(timeutil.MANILA)
    minutes = now.hour * 60 + (0 if now.minute < 30 else 30) + hours * 60
    minutes = max(0, min(minutes, 23 * 60 + 30))
    return time(minutes // 60, minutes % 60)


def seed(db: Session) -> None:
    today = timeutil.today()
    now = timeutil.now()
    # Open queues opened a couple of hours ago (by 8 AM at the latest) and close a few hours
    # from now (5 PM at the earliest)
    opened = min(hours_from_now(-2), time(8, 0))
    closes = max(hours_from_now(5), time(17, 0))

    # One hash for the crowd: bcrypt is slow on purpose, and there are hundreds of them
    crowd_hash = hash_password(DEMO_PASSWORD)
    crowd_phones = count(1_000_000)

    def account(name: str, phone: str, **fields) -> User:
        # The id is set here, not left to the database, so tickets can point at the account
        # before anything is written
        user = User(id=new_id(), name=name, phone=phone, password_hash=crowd_hash,
                    device_install_id="seed", **fields)
        db.add(user)
        return user

    # ---- Accounts ---------------------------------------------------------------
    maria = account("Maria Santos", "09175550001")
    rhea = account("Rhea Cruz", "09175550002", verification_status=VerificationStatus.VERIFIED,
                   organization_name="Brgy. Poblacion Council")
    account("Jun Dela Cruz", "09180000000", status=UserStatus.SUSPENDED,
            suspended_reason="Posting a fake queue",
            suspended_at=datetime(2026, 9, 28, 9, 0, tzinfo=timeutil.MANILA),
            verification_status=VerificationStatus.REVOKED)
    account("Quapp Admin", "09990000000", is_admin=True)

    organizer_phones = count(5550010)

    def organizer(org: str, verified: bool) -> User:
        return account(org, f"0917{next(organizer_phones)}",
                       verification_status=(VerificationStatus.VERIFIED if verified
                                             else VerificationStatus.NONE),
                       organization_name=org if verified else None)

    city_health = organizer("City Health Office", True)
    db.flush()

    queues: dict[str, Queue] = {}

    def queue(id: str, owner: User, line: Line, **fields) -> Queue:
        q = Queue(id=id, organizer_id=owner.id, status=fields.pop("status", QueueStatus.OPEN),
                  **fields)
        db.add(q)
        db.flush()
        queues[id] = q
        fill(q, line)
        return q

    def crowd_name(i: int) -> str:
        return f"{FIRST_NAMES[i % len(FIRST_NAMES)]} {LAST_NAMES[(i * 3) % len(LAST_NAMES)]}"

    def crowd_member(i: int) -> User:
        return account(crowd_name(i), f"0917{next(crowd_phones)}")

    def ticket(q: Queue, holder: User | None, name: str, **fields) -> Ticket:
        number = q.next_ticket_number
        q.next_ticket_number += 1
        t = Ticket(queue_id=q.id, user_id=holder.id if holder else None, holder_name=name,
                   holder_phone=holder.phone if holder else None, walk_in=holder is None,
                   ticket_number=number, line_order=float(number), **fields)
        db.add(t)
        return t

    def fill(q: Queue, line: Line) -> None:
        started = q.status in (QueueStatus.OPEN, QueueStatus.PAUSED)
        day = today if started else q.start_date
        served = line.served
        if started and line.waiting and line.served + line.no_shows < WARM_UP_SERVED:
            served = WARM_UP_SERVED - line.no_shows

        # Today so far: calls spaced by the minutes per person the wait aims for, the last
        # one a moment ago. Every so often one of them was a no-show.
        per_person = line.eta_minutes / line.waiting if line.waiting else 5
        finished = served + line.no_shows
        no_show_every = finished // line.no_shows if line.no_shows else 0
        for i in range(finished):
            called_at = now - timedelta(minutes=per_person * (finished - i))
            no_show = bool(no_show_every) and i % no_show_every == no_show_every - 1
            person = crowd_member(i)
            ticket(q, person, person.name, service_date=day,
                   status=TicketStatus.NO_SHOW if no_show else TicketStatus.SERVED,
                   joined_at=called_at - timedelta(minutes=40), called_at=called_at,
                   finished_at=called_at + timedelta(minutes=per_person))

        # The line: the first in line joined longest ago; every seventh is a walk-in, and the
        # fourth asked to move back (FakeData.seed)
        for i in range(line.waiting):
            walk_in = i % 7 == 6
            holder = None if walk_in else crowd_member(i)
            name = holder.name if holder else crowd_name(i)
            ticket(q, holder, name, service_date=day, status=TicketStatus.WAITING,
                   joined_at=now - timedelta(minutes=3 * (line.waiting - i)),
                   moved_back=i == 3,
                   moved_back_at=now - timedelta(minutes=12) if i == 3 else None)

    # ---- Queues (FakeData q1 to q12) --------------------------------------------
    queue("q1", rhea, Line(42, 55, 18, 3),
          name="Barangay Relief Distribution", category=Category.RELIEF,
          short_description="Family food packs for registered households",
          details="Distribution of family food packs for registered households. One pack per household.",
          bring="Barangay ID or proof of residency · claim stub",
          venue="Brgy. Poblacion Hall", municipality="Tagbilaran City",
          latitude=9.6496, longitude=123.8547,
          start_date=today, end_date=today + timedelta(days=1), opens_at=opened, closes_at=closes,
          grace_period_enabled=True, no_show_cooldown_enabled=True,
          proximity_check_enabled=True, join_radius_meters=1000)
    queue("q2", city_health, Line(18, 25),
          name="Free Medical Mission", category=Category.MEDICAL,
          short_description="Free check-ups, BP tests, and medicines",
          details="General consultation, blood pressure screening, and free maintenance medicine for seniors.",
          bring="Senior citizen ID if you have one",
          venue="Tagbilaran City Gym", municipality="Tagbilaran City",
          latitude=9.6543, longitude=123.8601,
          start_date=today, end_date=today, opens_at=opened, closes_at=closes,
          grace_period_enabled=True)
    queue("q3", rhea, Line(7, 12, 11, 1),
          name="Barangay Clearance Processing", category=Category.GOVERNMENT,
          short_description="Clearance for work and business permits",
          details="Application and release of barangay clearance for employment and business permits.",
          bring="Valid ID · ₱50 fee",
          venue="Brgy. Cogon Office", municipality="Tagbilaran City",
          latitude=9.6612, longitude=123.8578,
          start_date=today, end_date=today, opens_at=opened, closes_at=closes,
          grace_period_enabled=True)
    queue("q4", organizer("BISU Registrar", True), Line(63, 90),
          name="Registrar Enrollment Window 2", category=Category.EDUCATION,
          short_description="2nd semester enrollment, continuing students",
          details="Second semester enrollment for continuing students. Have your registration form pre-filled.",
          bring="Pre-filled registration form",
          venue="BISU Main Campus", municipality="Tagbilaran City",
          latitude=9.6402, longitude=123.8563,
          start_date=today, end_date=today + timedelta(days=4), opens_at=opened, closes_at=closes,
          grace_period_enabled=True, no_show_cooldown_enabled=True)
    queue("q5", rhea, Line(),
          name="Senior Citizen Pension Payout", category=Category.GOVERNMENT,
          short_description="Quarterly payout for registered senior citizens",
          details="Quarterly social pension release. Beneficiaries must claim in person or through an authorized representative.",
          bring="Senior citizen ID · authorization letter for representatives",
          venue="Brgy. Dao Covered Court", municipality="Tagbilaran City",
          latitude=9.6689, longitude=123.8695,
          start_date=today, end_date=today, opens_at=time(7, 0), closes_at=time(10, 0),
          status=QueueStatus.CLOSED, closed_at=now - timedelta(hours=1),
          grace_period_enabled=True, no_show_cooldown_enabled=True,
          proximity_check_enabled=True, join_radius_meters=500)
    queue("q6", organizer("Dauis Municipal Agriculture Office", False), Line(11, 18),
          name="Anti-Rabies Vaccination Drive", category=Category.MEDICAL,
          short_description="Free rabies shots for dogs and cats",
          details="Free anti-rabies vaccination for dogs and cats. One pet per queue slot.",
          bring="Your pet on a leash or in a carrier",
          venue="Brgy. Booy Health Center", municipality="Dauis",
          latitude=9.6236, longitude=123.8478,
          start_date=today + timedelta(days=1), end_date=today + timedelta(days=1),
          opens_at=time(8, 0), closes_at=time(14, 0), status=QueueStatus.UPCOMING,
          grace_period_enabled=True)
    queue("q7", organizer("Panglao Tourism Office", True), Line(5, 8),
          name="Tourist Assistance Desk", category=Category.OTHER,
          short_description="Lost items, transport help, referrals",
          details="Walk-in assistance for lost items, transport help, and accommodation referrals.",
          venue="Alona Beach Info Center", municipality="Panglao",
          latitude=9.5786, longitude=123.7486,
          start_date=today, end_date=today + timedelta(days=30),
          opens_at=time(9, 0), closes_at=time(18, 0))
    queue("q8", organizer("Baclayon MSWDO", True), Line(87, 120),
          name="Cash Aid Payout", category=Category.RELIEF,
          short_description="AICS financial assistance release",
          details="AICS financial assistance release. Claimants must present the notice sent by the MSWDO.",
          bring="MSWDO notice · valid ID",
          venue="Baclayon Municipal Hall", municipality="Baclayon",
          latitude=9.6244, longitude=123.9128,
          start_date=today, end_date=today + timedelta(days=2), opens_at=opened, closes_at=closes,
          status=QueueStatus.PAUSED, paused_at=now - timedelta(minutes=20),
          grace_period_enabled=True, no_show_cooldown_enabled=True,
          proximity_check_enabled=True, join_radius_meters=2000)
    queue("q9", organizer("Corella MSWDO", True), Line(3, 6),
          name="Solo Parent ID Application", category=Category.IDS,
          short_description="New and renewal solo parent IDs",
          details="New applications and renewals for solo parent identification cards.",
          bring="Birth certificate of child · barangay certificate",
          venue="Corella Municipal Hall", municipality="Corella",
          latitude=9.7089, longitude=123.9161,
          start_date=today + timedelta(days=2), end_date=today + timedelta(days=2),
          opens_at=time(9, 0), closes_at=time(15, 0), status=QueueStatus.UPCOMING,
          no_show_cooldown_enabled=True)
    queue("q10", organizer("Purok 3 Youth Volunteers", False), Line(29, 40),
          name="School Supplies Distribution", category=Category.EDUCATION,
          short_description="Notebooks and school kits, Grades 1–6",
          details="Distribution of notebooks and school kits for Grade 1–6 pupils. Parent or guardian must be present.",
          bring="Pupil's school ID or report card",
          venue="Loon Central Elementary", municipality="Loon",
          latitude=9.7986, longitude=123.7947,
          start_date=today, end_date=today, opens_at=opened, closes_at=closes,
          grace_period_enabled=True, no_show_cooldown_enabled=True,
          proximity_check_enabled=True, join_radius_meters=1000)
    queue("q11", organizer("Bohol Water Utilities", True), Line(14, 20),
          name="Water District Bill Payment", category=Category.BILLS,
          short_description="Pay your monthly water bill",
          venue="BWUA Office", municipality="Tagbilaran City",
          latitude=9.6478, longitude=123.8531,
          start_date=today, end_date=today + timedelta(days=60),
          opens_at=time(8, 0), closes_at=time(17, 0),
          grace_period_enabled=True)
    queue("q12", organizer("PESO Bohol", True), Line(6, 10),
          name="PESO Job Fair", category=Category.JOBS,
          short_description="Local and overseas hiring, walk-in interviews",
          details="Employers from Bohol and Cebu hiring on the spot. Bring several copies of your résumé.",
          bring="Résumé (5 copies) · valid ID",
          venue="Island City Mall Activity Center", municipality="Tagbilaran City",
          latitude=9.6617, longitude=123.8703,
          start_date=today + timedelta(days=1), end_date=today + timedelta(days=1),
          opens_at=time(13, 0), closes_at=time(17, 0), status=QueueStatus.UPCOMING,
          grace_period_enabled=True)

    # ---- Maria's past visits, so Queue history isn't empty (FakeData.pastTicket) ----
    for queue_id, status, days_ago in [("q2", TicketStatus.SERVED, 2),
                                       ("q5", TicketStatus.QUEUE_CLOSED, 9),
                                       ("q6", TicketStatus.NO_SHOW, 14),
                                       ("q3", TicketStatus.SERVED, 30)]:
        finished_at = now - timedelta(days=days_ago)
        ticket(queues[queue_id], maria, maria.name,
               service_date=finished_at.astimezone(timeutil.MANILA).date(), status=status,
               joined_at=finished_at - timedelta(minutes=40), finished_at=finished_at)

    db.commit()


def empty_all_tables(db: Session) -> None:
    """Deletes every row, children before parents so no foreign key is left dangling."""
    for table in reversed(Base.metadata.sorted_tables):
        db.execute(table.delete())
    db.commit()


if __name__ == "__main__":
    Base.metadata.create_all(engine)
    with SessionLocal() as session:
        empty_all_tables(session)
        seed(session)
        print(f"Seeded {session.query(Queue).count()} queues, "
              f"{session.query(Ticket).count()} tickets, {session.query(User).count()} accounts.")
