(ns imdbparse.movies
  (:require
    [clojure.string :as str])
  (:use
    imdbparse.util))

;; { :type  =>   :series | :episode | :movie | :video | :tv-movie | :videogame
;;   :id    =>   str,  unique item id
;;   :ident =>   str,  optional, part of id that comes after title, like "(2010)" or "(2011/II)"
;;   :title =>   str,  item title. For epsiode this is a title of enclosing series
;;   :year  =>   long, year of release. For series, this is starting year of the series
;;   :endyear => long, only for series, optional. Ending year of the series. If omitted, series is still running
;;   :series  => str,  only for episode. Id of enclosing series
;;   :eptitle => str,  only for episode, optional. Title of episode
;;   :season  => long, only for episode, optional. Season number
;;   :episode => long, only for episode, optional. Episode number
;; }

(defn parse-nums [m]
  (reduce (fn [m k] (if (m k)
                      (update-in m [k] parse-long)
                      m))
          m
          [:year :endyear :season :episode]))

(def re-year "(\\d{4}|\\?{4})")
(def re-ident (str "\\((?<ident>" re-year "(/[IVX]+)?)\\)"))
(def re-category "\\((?<category>V|TV|VG)\\)")
(def categories {"V"  :video
                 "TV" :tv-movie
                 "VG" :videogame})

(def re-series-title (str "\"(?<title>.*)\"\\s" re-ident)) ;; "The Thick of It" (2014)
(def re-series-years (str "(?<year>\\d{4})-(?<endyear>" re-year ")")) ;; 2013-2014 or 2013-????
(def re-series (re-pattern (str "(?<id>" re-series-title ")\\s+" re-series-years)))

(def re-episode-info
  (str "\\{("
           "(?<eptitle1>.+)" "\\s" "\\(\\#" "(?<season1>\\d+)" "\\." "(?<episode1>\\d+)" "\\)"
       "|" "\\(\\#" "(?<season2>\\d+)" "\\." "(?<episode2>\\d+)" "\\)"
       "|" "(?<eptitle3>.+)"
       ")\\}")) ;; "{Episode title (#7.18)}" or "{(#7.18)}" or {Episode title}
(def re-episode (re-pattern (str "(?<id>(?<series>" re-series-title ")\\s" re-episode-info ")" "\\s+(?<year>" re-year ")")))

(def re-movie-title (str "(?<id>(?<title>.*)\\s" re-ident "(\\s" re-category ")?)"))
(def re-movie (re-pattern (str re-movie-title "\\s+(?<year>" re-year ")")))

(defn parse-movies-line [line]
  (when-not (re-find #"SUSPENDED" line)
    (or
      (some->
        (condp re-matches-map line
          re-series  :>> #(assoc % :type :series)
          re-episode :>> #(assoc % :type :episode)
          re-movie   :>> #(-> %
                              (assoc :type (get categories (:category %) :movie))
                              (dissoc :category)))
        parse-nums)
      (println "\n    BAD LINE:" line))))

(defn movies-seq [lines-seq]
  (map parse-movies-line lines-seq))

(defn reencode [file]
  (reencode-ll
    :file   file
    :seq-fn movies-seq
    :begin  ["MOVIES LIST" "===========" ""]
    :end    #"-{10,}"))
  
;;;; TEST 

(use 'clojure.test)
  

(deftest test-series
  (is (= (parse-movies-line "\"#InstaFamous: How\" (2013)  2013-2014")
         {:type :series, :id "\"#InstaFamous: How\" (2013)", :ident "2013",
          :title "#InstaFamous: How", :year 2013, :endyear 2014}))
  (is (= (parse-movies-line "\"#InstaFamous: How\" (2013)  2013-????")
         {:type :series, :id "\"#InstaFamous: How\" (2013)", :ident "2013",
          :title "#InstaFamous: How", :year 2013})))

(deftest test-episodes
  (is (= (parse-movies-line "\"#InstaFamous: How\" (2013) {Elf on the Shelf (#1.2)}  2013")
         {:type :episode, :id "\"#InstaFamous: How\" (2013) {Elf on the Shelf (#1.2)}", :ident "2013"
          :series "\"#InstaFamous: How\" (2013)"
          :title "#InstaFamous: How", :eptitle "Elf on the Shelf", :season 1, :episode 2, :year 2013}))
  (is (= (parse-movies-line "\"#LakeShow\" (2012) {(2012-10-01)}     2012")
         {:type :episode, :id "\"#LakeShow\" (2012) {(2012-10-01)}", :ident "2012", 
          :series "\"#LakeShow\" (2012)"
          :title "#LakeShow", :eptitle "(2012-10-01)", :year 2012}))
  (is (= (parse-movies-line "\"The Thick of It\" (2005) {(#4.7)}			2012")
         {:type :episode, :id "\"The Thick of It\" (2005) {(#4.7)}", :ident "2005", 
          :series "\"The Thick of It\" (2005)"
          :title "The Thick of It", :season 4, :episode 7, :year 2012}))
  (is (= (parse-movies-line "\"The Thick of It\" (2005) {Spinners: Extra}	2007")
         {:type :episode, :id "\"The Thick of It\" (2005) {Spinners: Extra}", :ident "2005",
          :series "\"The Thick of It\" (2005)"
          :title "The Thick of It", :eptitle "Spinners: Extra", :year 2007}))
  (is (= (parse-movies-line "\"$9.99\" (2003) {(#5.5)}					????")
         {:type :episode, :id "\"$9.99\" (2003) {(#5.5)}", :ident "2003", 
          :series "\"$9.99\" (2003)"
          :title "$9.99", :season 5, :episode 5}))
  (is (= (parse-movies-line "\"Zetsuen No Tempest\" (2012) {Femme Fatale (Woman of Fate) (#1.21)}	2013")
         {:type :episode, :id "\"Zetsuen No Tempest\" (2012) {Femme Fatale (Woman of Fate) (#1.21)}", :ident "2012", 
          :series "\"Zetsuen No Tempest\" (2012)"
          :title "Zetsuen No Tempest", :eptitle "Femme Fatale (Woman of Fate)", :season 1, :episode 21, :year 2013}))
  (is (= (parse-movies-line "\"WRAL Murder Trials\" (2003) {Ryan: Thomas (Pt 3)}	2010")
         {:type :episode, :id "\"WRAL Murder Trials\" (2003) {Ryan: Thomas (Pt 3)}", :ident "2003", 
          :series "\"WRAL Murder Trials\" (2003)"
          :title "WRAL Murder Trials", :eptitle "Ryan: Thomas (Pt 3)", :year 2010}))
  )

(deftest test-movies
  (is (= (parse-movies-line "ça promet ! (2011)          2011")
         {:type :movie, :id "ça promet ! (2011)", :ident "2011",
          :title "ça promet !", :year 2011}))
  (is (= (parse-movies-line "éX-Driver: Nina & Rei Danger Zone (2002) (V)    2002")
	       {:type :video, :id "éX-Driver: Nina & Rei Danger Zone (2002) (V)", :ident "2002",
          :title "éX-Driver: Nina & Rei Danger Zone", :year 2002}))
  (is (= (parse-movies-line "New Year's Eve (2013/II)					2013")
         {:type :movie, :id "New Year's Eve (2013/II)", :ident "2013/II", 
          :title "New Year's Eve", :year 2013}))
  (is (= (parse-movies-line "Broken (????/III)					????")
         {:type :movie, :id "Broken (????/III)", :ident "????/III", 
          :title "Broken"})))

#_(test-ns 'imdbparse.movies)
