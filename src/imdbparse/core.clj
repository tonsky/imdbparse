(ns imdbparse.core
  (:require
    [clojure.string :as str]
    [imdbparse.movies :as movies]
    [imdbparse.genres :as genres]
    [imdbparse.ratings :as ratings]
    [imdbparse.people :as people])
  (:use
    imdbparse.util))

(defn reencode [dir]
  (movies/reencode (str dir "/movies.list"))
  (genres/reencode (str dir "/genres.list"))
  (ratings/reencode (str dir "/ratings.list"))
  (people/reencode (str dir "/directors.list"))
  (people/reencode (str dir "/writers.list"))
  (people/reencode (str dir "/cinematographers.list"))
  (people/reencode (str dir "/composers.list"))
  (people/reencode (str dir "/actors.list"))
  (people/reencode (str dir "/actresses.list"))
)

(defn -main [dir]
  (reencode dir))



