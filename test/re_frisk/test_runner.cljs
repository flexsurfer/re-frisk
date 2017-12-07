(ns re-frisk.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [re-frisk.diff-test]
            [re-frisk.delta-test]))

(doo-tests 're-frisk.delta-test
           're-frisk.diff-test)
