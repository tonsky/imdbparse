(ns imdbparse.ratings
  (:require
    [clojure.string :as str])
  (:use
    imdbparse.util))

;; { :id     => str, movie id
;;   :dist   => str, votes distribution, like "00.0112201"
;;   :votes  => long, amount of votes
;;   :rating => double, average rating }


(def re-rating #"(?x)\s+ (?<dist>[0-9\.\*]{10})
                     \s+ (?<votes>\d+)
                     \s+ (?<rating>\d{1,2}\.\d)
                     \s+ (?<id>.*)")

(defn parse-ratings-line [line]
  (or
    (some-> (re-matches-map re-rating line)
      (update-in [:votes] parse-long)
      (update-in [:rating] parse-double))
    (println "\n    BAD LINE:" line)))

(defn ratings-seq [lines-seq]
  (map parse-ratings-line lines-seq))

(defn reencode [file]
  (reencode-ll
    :file   file
    :seq-fn ratings-seq
    :begin  ["MOVIE RATINGS REPORT" "" "New  Distribution  Votes  Rank  Title"]
    :end     #"^$"))


;;;; TEST
 
(use 'clojure.test)

(deftest test-parse-ratings-line
  (is (= (parse-ratings-line "        00.0112201      65   6.9  \"$#*! My Dad Says\" (2010) {Code Ed (#1.4)}")
         { :dist "00.0112201"
           :votes 65
           :rating 6.9
           :id "\"$#*! My Dad Says\" (2010) {Code Ed (#1.4)}" }))
  (is (= (parse-ratings-line "      .........*       7  10.0  \"14.000.000.000$!!!\" (2013) {(#2.6)}")
         { :dist ".........*"
           :votes 7
           :rating 10.0
           :id "\"14.000.000.000$!!!\" (2013) {(#2.6)}" })))

#_(test-ns 'imdbparse.ratings)
