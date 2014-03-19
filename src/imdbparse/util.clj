(ns imdbparse.util
  (:require
    [clojure.string :as str]
    [clojure.java.io :as io])
  (:use
    clojure.test))

(defn- group->key [s]
  (-> (re-matches #"(.*?)\d?" s)
      second
      keyword))

(defn re-matches-map
  "Given regex with named groups, returns map from group names to matches.
   E.g. (re-matches-map #\"(?<id>[0-9]+)(?<rest>.*)\" \"123abc\") =>
          => { :id \"123\" :rest \"abc\" }
   If group name ends with number, it's truncared, so groups can be used in options:
     (re-matches-map #\"((?<id1>[0-9]+)|(?<id2>[a-z]+))\" ...) => { :id \"...\") }"
  [re s]
  (let [groups (->> (.pattern re)
                    (re-seq #"\?<(\w+)>")
                    (map second))
        matcher (re-matcher re s)]
    (when (.matches matcher)
      (into {}
        (for [g groups
              :let [match (.group matcher g)]
              :when (and match
                         (not= match "????"))]
          [(group->key g) match])))))


(defn- consume-prefix [^java.io.BufferedReader rdr from-lines]
  (loop [waiting from-lines]
    (when waiting
      (when-let [line (.readLine rdr)]
        (if (= line (first waiting))
          (recur (next waiting))
          (recur from-lines))))))
    
(defn- consume-until [^java.io.BufferedReader rdr till-re]
  (when-let [line (.readLine rdr)]
    (when-not (re-matches till-re line)
      (cons line (lazy-seq (consume-until rdr till-re))))))

(defn lines-seq
  "This is used to lazily consume reader line-by-line, but skip file prefix and drop file suffix.
   Prefix is set of lines, suffix is regexp that should be met by line."
  [rdr from-lines till-re]
  (consume-prefix rdr from-lines)
  (consume-until rdr till-re))


(defn parse-long [s]
  (when-not (str/blank? s)
    (Long/parseLong s)))

(defn parse-double [s]
  (when-not (str/blank? s)
    (Double/parseDouble s)))


(defn split-by [f seq]
  (loop [taken []
         tail  seq]
    (let [el (first tail)]
      (if (and el (or (empty? taken) (f taken el)))
        (recur (conj taken el) (next tail))
        [taken tail]))))

(defn collect-by
  "Lazily transform `seq`, grouping items by predicate `split-fn`
   and then post-process groups by `collect-fn`."
  [split-fn collect-fn seq]
  (when seq
    (let [[head tail] (split-by split-fn seq)]
      (cons (collect-fn head) (lazy-seq (collect-by split-fn collect-fn tail))))))

(defmacro doseq-progress
  "Doseq macro that also prints progress to stdout"
  [bindings & body]
 `(let [pos# (atom 0)]
    (doseq ~bindings
      ~@body
      (when (= 0 (mod @pos# 10000))
        (print ".")
        (flush))
      (swap! pos# inc))
    (println " DONE")))

(defn reencode-ll [& {:keys [file seq-fn begin end]}]
  (let [out (str file ".edn")]
    (if (.exists (io/as-file out))
      (println "[ SKIPPING ] File exists:" out)
      (try
        (with-open [rdr (io/reader file :encoding "windows-1252")
                    wr  (io/writer out)]
          (print "[ CONVERTING ]" file "to" out "") (flush)
          (doseq-progress [parsed (seq-fn (lines-seq rdr begin end))
                           :when parsed]
            (.write wr (pr-str parsed))
            (.write wr "\n")))
        (catch java.io.FileNotFoundException e
          (println "[ SKIPPING ] No file:" file))))))
