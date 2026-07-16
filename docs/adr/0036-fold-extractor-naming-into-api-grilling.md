# Fold issue 18 into the pre-1.0 API grilling

Issue #18 is not a separate extractor-merge implementation or release blocker. Its maintainer comment establishes that
source extraction and AST extraction run in different phases, so merging them is unnecessary; the remaining naming and
facade concern belongs in the mandatory pre-1.0 supported-API grilling. The authorized curation pass closed #18 as
superseded by grilling issue #37 and implementation issue #38.
