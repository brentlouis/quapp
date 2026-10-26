# Asset tools

- `rebuild-gallery.ps1` rebuilds the 64-screen HTML gallery and ZIP from the artboard files and notes. The gallery embeds local fonts for offline viewing.

- `make_logo.py` is the source for the Quapp ticket mark, launcher foreground, and monochrome notification/launcher artwork. Edit the generator rather than generated vector files.
- `make_grain.py` generates the paper and ink texture assets used by Civic Paper.

Run generators only when intentionally changing the design system. Review generated diffs and visually inspect the affected Android screens afterward.
