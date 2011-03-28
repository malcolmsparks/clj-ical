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

(defproject clj-ical "1.1"
  :description "A library to print iCalendar objects as defined by RFC 2445."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clj-time "0.1.0-RC1"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [marginalia "0.5.0"]])
