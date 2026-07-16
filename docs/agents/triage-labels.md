# Triage roles

Use these canonical roles when describing issue state:

| Canonical role | Meaning |
|---|---|
| `needs-triage` | Maintainer must evaluate scope or priority |
| `needs-info` | More reporter or environment evidence is required |
| `ready-for-agent` | Intent and executable acceptance are confirmed for autonomous implementation |
| `ready-for-human` | The work requires maintainer judgment or execution |
| `wontfix` | The repository has decided not to pursue the request |

These label names are present in the repository and intentionally match the shared KlumAST triage vocabulary. When a
workflow refers to a canonical role, use the corresponding label exactly; do not substitute a superficially similar
default GitHub label.
