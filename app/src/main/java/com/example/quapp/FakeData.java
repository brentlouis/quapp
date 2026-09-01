package com.example.quapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FakeData {

    private FakeData() {
        // Utility class.
    }

    public static Queue queueById(String queueId) {
        for (Queue queue : queues()) {
            if (queue.getId().equals(queueId)) {
                return queue;
            }
        }
        return null;
    }

    public static List<Queue> ownedQueues() {
        return new ArrayList<>(Arrays.asList(
                queueById("q1"),
                queueById("q3"),
                queueById("q5")
        ));
    }

    public static List<Ticket> ticketsForQueue(Queue queue) {
        List<Ticket> tickets = new ArrayList<>();

        String[] names = {
                "Maria Santos", "Jose Rivera", "Ana Lopez", "Pedro Cruz",
                "Rosa Mendoza", "Carlos Bautista", "Elena Torres", "Miguel Reyes"
        };

        for (int i = 0; i < names.length; i++) {
            tickets.add(new Ticket(
                    "t" + (i + 1),
                    queue.getId(),
                    queue.getName(),
                    queue.getVenue(),
                    names[i],
                    "0917" + (1000000 + i),
                    i + 1,
                    i + 1,
                    (i + 1) * 5,
                    Ticket.Status.WAITING));
        }

        return tickets;
    }

    public static List<Queue> queues() {
        return new ArrayList<>(Arrays.asList(
                new Queue("q1", "Barangay Relief Distribution", "Brgy. Poblacion Hall",
                        "Tagbilaran City", 9.6496, 123.8547,
                        "Relief",
                        "Distribution of family food packs for registered households. Bring a valid ID and your barangay certificate.",
                        "Aug 30–31, 8:00 AM – 4:00 PM",
                        42, 55, true,
                        true, true, true, true),

                new Queue("q2", "Free Medical Mission", "Tagbilaran City Gym",
                        "Tagbilaran City", 9.6543, 123.8601,
                        "Medical",
                        "General consultation, blood pressure screening, and free maintenance medicine for seniors.",
                        "Saturday, 7:00 AM – 12:00 NN",
                        18, 25, true,
                        true, true, false, false),

                new Queue("q3", "Barangay Clearance Processing", "Brgy. Cogon Office",
                        "Tagbilaran City", 9.6612, 123.8578,
                        "Government",
                        "Application and release of barangay clearance for employment and business permits.",
                        "Mon–Fri, 8:00 AM – 5:00 PM",
                        7, 12, true,
                        false, true, false, false),

                new Queue("q4", "Registrar Enrollment Window 2", "BISU Main Campus",
                        "Tagbilaran City", 9.6402, 123.8563,
                        "Education",
                        "Second semester enrollment for continuing students. Have your registration form pre-filled.",
                        "Mon–Fri, 8:00 AM – 4:00 PM",
                        63, 90, true,
                        false, true, true, false),

                new Queue("q5", "Senior Citizen Pension Payout", "Brgy. Dao Covered Court",
                        "Tagbilaran City", 9.6689, 123.8695,
                        "Government",
                        "Quarterly social pension release. Beneficiaries must claim in person or through an authorized representative.",
                        "Sept 5, 8:00 AM – 3:00 PM",
                        0, 0, false,
                        true, true, true, true),

                new Queue("q6", "Anti-Rabies Vaccination Drive", "Brgy. Booy Health Center",
                        "Dauis", 9.6236, 123.8478,
                        "Medical",
                        "Free anti-rabies vaccination for dogs and cats. One pet per queue slot.",
                        "Sept 3, 8:00 AM – 2:00 PM",
                        11, 18, true,
                        false, true, false, false),

                new Queue("q7", "Tourist Assistance Desk", "Alona Beach Info Center",
                        "Panglao", 9.5786, 123.7486,
                        "Community",
                        "Walk-in assistance for lost items, transport help, and accommodation referrals.",
                        "Daily, 9:00 AM – 6:00 PM",
                        5, 8, true,
                        false, false, false, false),

                new Queue("q8", "Cash Aid Payout", "Baclayon Municipal Hall",
                        "Baclayon", 9.6244, 123.9128,
                        "Relief",
                        "AICS financial assistance release. Claimants must present the notice sent by the MSWDO.",
                        "Sept 2–4, 8:00 AM – 5:00 PM",
                        87, 120, true,
                        true, true, true, true),

                new Queue("q9", "Solo Parent ID Application", "Corella Municipal Hall",
                        "Corella", 9.7089, 123.9161,
                        "Government",
                        "New applications and renewals for solo parent identification cards.",
                        "Tue & Thu, 9:00 AM – 3:00 PM",
                        3, 6, true,
                        false, false, true, false),

                new Queue("q10", "School Supplies Distribution", "Loon Central Elementary",
                        "Loon", 9.7986, 123.7947,
                        "Education",
                        "Distribution of notebooks and school kits for Grade 1–6 pupils. Parent or guardian must be present.",
                        "Sept 8, 7:00 AM – 11:00 AM",
                        29, 40, true,
                        false, true, true, true)
        ));
    }
}