# disruptor

A Clojure DSL for [LMAX Disruptor](https://github.com/LMAX-Exchange/disruptor).

## Usage

```clj
(require '[disruptor.core :as d])

(def disruptor (d/disruptor {:size 8})) ;; Size must be a power of 2

(core/add-handlers! disruptor {:handlers [(fn [{:keys [event]}] ...)]})

(d/start! disruptor)
(d/publish! disruptor {:event {:foo :bar}})
(d/shutdown! disruptor)
```

## License

Copyright © 2022 Haokang Den

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
