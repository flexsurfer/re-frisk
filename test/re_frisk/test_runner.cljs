(ns re-frisk.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [re-frisk.diff-test]))

(doo-tests 're-frisk.diff-test)
