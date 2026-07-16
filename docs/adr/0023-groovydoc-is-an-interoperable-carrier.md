# Accept runtime GroovyDoc as an interoperable carrier

AnnoDocimal accepts Groovy's runtime GroovyDoc annotation as an alternate documentation input. AnnoDocimal's own
documentation protocol remains the canonical cross-language carrier and the evolution point for structured
documentation. Capture, extraction, and projection must use documented deterministic precedence when more than one
carrier is present and must avoid emitting duplicate representations merely because GroovyDoc is enabled. Issue #19
owns the compatibility details and migration of its now-partly-stale checklist.
