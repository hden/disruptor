.PHONY: lint test repl duct-setup install-jars

lint:
	clj-kondo --parallel --lint src test

test:
	lein test

coverage:
	lein coverage

repl:
	lein repl
