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

(ns
    ^{:doc "A library for printing iCalendar objects as specified by [RFC 2445](http://rfc.ietf.org/rfc/2445.txt)."
      :author "Malcolm Sparks"}
  clj-ical.format
  (:use clojure.string)
  (:refer-clojure :exclude [replace reverse println])
  (:require [clj-time.format :as format]))

(def ^{:doc "The media-type of all iCalendar objects, as declared in
     section 3.1"}
     media-type "text/calendar")

(def ^{:dynamic true} *fold-column* 75)

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

;; ## Value and property parameter printing.

;; 4.1.1 Property parameters with values containing a COLON, a
;; SEMICOLON or a COMMA character MUST be placed in quoted text.
(defn quote-if-necessary [x]
  (if (some #{\: \; \,} x) (str "\"" x "\"") x))

(defn is-quoted? [x]
  (let [s (seq x)]
    (and
     (= \" (first s))
     (not (some #{\"} (butlast (rest s)))))))

(defmulti format-value class :default :other)

(defmethod format-value String [s] s)
(defmethod format-value org.joda.time.LocalDate [ld]
           (.print (clj-time.format/formatters :basic-date) ld))
(defmethod format-value org.joda.time.DateTime [dt]
           (.print (clj-time.format/formatters :basic-date-time-no-ms) dt))
(defmethod format-value :other [s] s)


(defmulti format-parameter-value class :default :other)

(defmethod format-parameter-value Boolean [x]
           (upper-case (str x)))

(defmethod format-parameter-value org.joda.time.LocalDate [x]
           (format-value x))

(defmethod format-parameter-value org.joda.time.DateTime [x]
           (format-value x))

(defmethod format-parameter-value :other [x]
           (cond
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

;; ## Writing objects

(defn write-object
  "Write an iCalendar object."
  [obj]
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
 (reduce str (interpose "," (map format-value (flatten values))))))))))



