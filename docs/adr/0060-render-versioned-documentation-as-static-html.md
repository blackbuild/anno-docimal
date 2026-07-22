# Render versioned documentation as deterministic static HTML

The shared KlumAST VD-5 presentation contract now supplies the cross-project design that ADR 0017 awaited, so
AnnoDocimal publishes exact release documentation as deterministic static HTML plus Javadocs rather than relying only
on tags and Maven artifacts. Authored user Markdown under `docs/user/` remains canonical, with `Home.md` supplying the
exact-tree landing. The repository README and change history are not renderer inputs. A pinned AnnoDocimal-local renderer owns chrome,
link rewriting, manifests, and credential-free crawling, while a distinct non-release rehearsal path proves presentation
without consuming single-use pending release evidence. The shared contract standardizes outcomes, not code or release
mechanics, so AnnoDocimal retains its repository-local implementation and protected publication ownership.
