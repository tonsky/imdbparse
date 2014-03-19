(defproject imdbparse "0.1.0-SNAPSHOT"
  :global-vars {*warn-on-reflection* true}
  :dependencies [
    [org.clojure/clojure "1.5.1"]
    [instaparse "1.3.0"]
  ]
  :main imdbparse.core)
