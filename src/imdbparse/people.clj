(ns imdbparse.people
  (:require
    [clojure.string :as str])
  (:use
    imdbparse.util))

;; { :id     => str, unique person id
;;   :name   => str, person name
;;   :ident  => str, optional, part of id that comes after name, like "(II)"
;;   :movies => map from movie id to
;;     { :as      => str, optional, role title, like "Himself" or "Dark Lord"
;;       :comment => str, optional, additional role description, like "voice" or "as Bob Johnson"
;;       :pos     => str, optional, billing credits position }


;; \n\n
;; (lastname, )?name\(ident\)?
;; \t+
;; (movie_id \s{2} (as_comment)? \s{2} [as]? \s{2} <pos>?\n)+

(defn parse-person-name [s]
  (let [{:keys [name lastname ident]} (re-matches-map #"(?x)
                                                        ( (?<lastname> [^,]+),\s)?
                                                        (?<name> [^()]+)
                                                        (\s \( (?<ident> [IVX]+) \) )?" s)]
    (cond->
      { :id s
        :name (str name (when lastname (str " " lastname))) }
      ident (assoc :ident ident))))
    

(defn parse-person-movie [s]
  (let [[movie & attrs] (str/split s #"\s{2,3}")
        attrs (try
                (reduce
                  (fn [res attr]
                    (condp re-matches attr
                      #"\((.*)\)" :>> #(assoc res :comment (second %))
                      #"\[(.*)\]" :>> #(assoc res :as (second %))
                      #"<(.*)>"   :>> #(assoc res :pos (second %))))
                  {}
                  attrs)
                (catch java.lang.IllegalArgumentException e
                  (println "\n    BAD LINE" s)))]
    [movie attrs]))
  

(defn parse-movie-block [lines]
  (let [[actor movie] (str/split (first lines) #"\t+")]
    (concat
      [actor movie]
      (->> lines
           next
           (remove str/blank?)
           (map #(re-find #"[^\t]+" %))))))

(defn parse-person [block]
  (let [[name & movies] block
        name   (parse-person-name name)
        movies (into {} (map parse-person-movie movies))]
    (assoc name 
      :movies movies)))

(defn person-seq [lines-seq]
  (let [blocks (collect-by 
                 (fn [coll el] (not= (last coll) ""))
                 (fn [coll] (parse-movie-block coll))
                 lines-seq)]
    (map parse-person blocks)))


(defn reencode [file]
  (reencode-ll
    :file   file
    :seq-fn person-seq
    :begin  ["----\t\t\t------"]
    :end     #"-{10,}"))


;;;; TEST

(use 'clojure.test)

(deftest test-parse-person-name
  (is (= (parse-person-name "$hort, Too")
         { :id   "$hort, Too"
           :name "Too $hort" }))
  (is (= (parse-person-name "*NSYNC")
         { :id   "*NSYNC"
           :name "*NSYNC" }))
  (is (= (parse-person-name "Cage, Nick (II)")
         { :id    "Cage, Nick (II)"
           :name  "Nick Cage"
           :ident "II" })))

(deftest test-parse-person-movie
  (is (= (parse-person-movie "\"Unsung\" (2008) {Too $hort}  [Himself]")
         [ "\"Unsung\" (2008) {Too $hort}"
           { :as "Himself" } ]))
  (is (= (parse-person-movie "Porndogs (2009)  (voice)  [Bosco]  <3>")
         [ "Porndogs (2009)"
           { :comment "voice"
             :as "Bosco"
             :pos "3" } ])))

(deftest test-person-seq
  (is (= (person-seq ["$, Claw			\"OnCreativity\" (2012)  [Himself]"
                      ""
                      "$, Homo			Nykytaiteen museo (1986)  [Himself]  <25>"
                      "			Suuri illusioni (1985)  [Guests]  <22>"
                      ""
                      "$, Steve		E.R. Sluts (2003) (V)  <12>"
                      ""])
    [{ :name "Claw $"
       :id "$, Claw"
       :movies {
         "\"OnCreativity\" (2012)" { :as "Himself" }
       }}
     { :name "Homo $"
       :id "$, Homo"
       :movies {
         "Nykytaiteen museo (1986)" { :as "Himself" :pos "25" }
         "Suuri illusioni (1985)"   { :as "Guests" :pos "22" }
       }}
     { :name "Steve $"
       :id "$, Steve"
       :movies {
         "E.R. Sluts (2003) (V)" { :pos "12" }
       }}])))

#_(test-ns 'imdbparse.people)

