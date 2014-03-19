(ns imdbparse.genres
  (:require
    [clojure.string :as str])
  (:use
    imdbparse.util))

;; { :id     => str, movie id
;;   :genres => set of str, genres }

(def re-genres #"(?x)(?<id>.*?)\s+(?<genre>[\w\-]+)")
(defn parse-genres-line [line]
  (or
    (re-matches-map re-genres line)
    (println "\n    BAD LINE:" line)))


(defn genres-seq [lines-seq]
  (let [genres-seq (->> lines-seq
                        (map parse-genres-line)
                        (remove nil?))]
    (collect-by
      (fn [coll el] (= (:id (first coll)) (:id el)))
      (fn [coll]    { :id     (:id (first coll))
                      :genres (set (map :genre coll)) })
      genres-seq)))


(defn reencode [file]
  (reencode-ll
    :file   file
    :seq-fn genres-seq
    :begin  ["8: THE GENRES LIST" "==================" ""]
    :end    #"^$"))

;;;; TEST

(use 'clojure.test)

(deftest test-parse-genres-line
  (is (= (parse-genres-line "Über Wasser: Menschen und gelbe Kanister (2007)		Documentary")
         { :id "Über Wasser: Menschen und gelbe Kanister (2007)"
           :genre "Documentary" }))
  (is (= (parse-genres-line "\"#Hashtag: The Series\" (2013)       Comedy")
         { :id "\"#Hashtag: The Series\" (2013)"
           :genre "Comedy" })))

(deftest test-genres-seq
  (is (= (genres-seq ["Über Wasser: Menschen und gelbe Kanister (2007)		Documentary"
                      "\"#Hashtag: The Series\" (2013)       Comedy"
                      "\"#Hashtag: The Series\" (2013)       Drama"])
    [{ :id "Über Wasser: Menschen und gelbe Kanister (2007)"
       :genres #{"Documentary"} }
     { :id "\"#Hashtag: The Series\" (2013)"
       :genres #{"Comedy" "Drama"} }])))
  

#_(test-ns 'imdbparse.genres)
