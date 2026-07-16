# Curate the transformation-author surface

The supported transformation-author API will use a curated facade and documentation value model rather than promoting
the current helper classes wholesale. Existing helpers remain provisional until a future design maps them to the curated
contract, retains them deliberately, or supplies appropriately versioned compatibility shims. This avoids freezing
metadata keys, protected mutable state, parsing machinery, and today's string representation before issue #30's possible
structured protocol evolution. This baseline does not design or implement the facade.
