;; Copyright 2011 Malcolm Sparks.
;;
;; This file is part of clj-ical.
;;
;; clj-ical is free software: you can redistribute it and/or modify it under the
;; terms of the GNU Affero General Public License as published by the Free
;; Software Foundation, either version 3 of the License, or (at your option) any
;; later version.
;;
;; clj-ical is distributed in the hope that it will be useful but WITHOUT ANY
;; WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
;; A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more
;; details.
;;
;; Please see the LICENSE file for a copy of the GNU Affero General Public License.

(ns clj-ical.format
  (:use clojure.string)
  (:refer-clojure :exclude [replace reverse println])
  (:require [clj-time.format :as format]))

(def media-type "text/calendar")

(def *fold-column* 75)

(defn println [s]
  (letfn [(fold [s n]
                (let [lines (partition n n nil s)]
                  (->>
                   (cons (first lines)
                         ;; Each subsequent line to begin with a space
                         (map (partial cons " ") (rest lines)))
                   (map #(concat % "\r\n")) ; Add CRLFs
                   ;; Convert each line back to a string
                   (map (partial reduce str))
                   (reduce str))))]
    (doall (print (fold s *fold-column*)))))

(defn forbid-quote-in-parameter-value [x]
  (if (some #{\"} x)
    (throw (Exception. "Parameter value cannot contain a double-quote")))
  x)

;; 4.1.1 Property parameters with values containing a COLON, a
;; SEMICOLON or a COMMA character MUST be placed in quoted text.
(defn quote-if-necessary [x]
  (if (some #{\: \; \,} x) (str "\"" x "\"") x))

(defn is-quoted? [x]
  (let [s (seq x)]
    (and
     (= \" (first s))
     (not (some #{\"} (butlast (rest s)))))))

()

(defn boolean? [x]
  (or (= x true) (= x false)))

(defn format-parameter-value [x]
  (cond
   (boolean? x) (upper-case (str x))
   (is-quoted? x) x
   :otherwise ((comp quote-if-necessary
                     upper-case
                     forbid-quote-in-parameter-value) x)))

(defmulti serialize-param coll?)
(defmethod serialize-param false [x]
           (format-parameter-value x))
(defmethod serialize-param true [x]
           (->> x
                (map format-parameter-value)
                (interpose ",")
                (reduce str)))

(defn ^{:private true} is-parent? [obj]
  (true? (some #(and (coll? %)
                     (keyword? (first %)))
               obj)))

(defn write-object [obj]
  (letfn [(serialize-params [params]
                            (map (fn [[k v]]
                                   (format "%s=%s"
                                           (upper-case (name k))
                                           (serialize-param v))) params))]
   (assert (keyword? (first obj)))
    (let [n (upper-case (name (first obj)))
          snd (second obj)
          params (if (map? snd) snd)
          values (drop (if params 2 1) obj)
          parent? (true?
                   (some #(and (vector? %) (keyword? (first %)))
                         values))]
      (if parent?
        (do
          (println (str "BEGIN:" n))
          (doall (for [e values] (write-object e)))
          (println (str "END:" n)))
        (println
         (str
          (reduce str (interpose ";" (cons n (serialize-params params))))
          ":"
          (reduce str (interpose "," (flatten values)))))))))

(defn datetime [dt]
  (clj-time.format/unparse
   (clj-time.format/formatters :basic-date-time-no-ms)
   dt))

