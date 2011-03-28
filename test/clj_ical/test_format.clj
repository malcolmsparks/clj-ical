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

(ns clj-ical.test-format
  (:use clojure.test)
  (:import
   [org.joda.time LocalDate LocalDateTime DateTimeZone DateTimeConstants DateMidnight]
   [org.joda.time.format ISODateTimeFormat])
  (:require clj-ical.format
            clj-time.format
            [clj-time.core :as time]))

(deftest test-ical
  (letfn
      [(to-ical [input] (with-out-str (clj-ical.format/write-object input)))
       (same [input expected] (= (to-ical input) (str expected "\r\n")))]

    (testing "Properties"
      (are [input expected] (same input expected)
           [:version "2.0"] "VERSION:2.0"))

    ;; RFC 2445 specifies that lines should be folded.
    (testing "Long lines"
      (are [input expected-line-count]
           (= expected-line-count
              (binding [clj-ical.format/*fold-column* 10]
                (count (line-seq (java.io.BufferedReader. (java.io.StringReader. (to-ical input)))))))
           [:summary "Bastille Day Party which is a big celebration in France and this line is only this long to test the folding algorithm."]
           13))

    (testing "Multiple parameters"
      (are [input expected] (same input expected)
           [:attendee {:rsvp true :role "req-participant"} "MAILTO:jsmith@host.com"]
           "ATTENDEE;RSVP=TRUE;ROLE=REQ-PARTICIPANT:MAILTO:jsmith@host.com"))

    (testing "Multiple value parameter"
      (are [input expected] (same input expected)
           [:attendee {:role ["req-participant", "author"]} "MAILTO:jsmith@host.com"]
           "ATTENDEE;ROLE=REQ-PARTICIPANT,AUTHOR:MAILTO:jsmith@host.com"))

    (testing "Unquoted parameter is upper case"
      (are [input expected] (same input expected)
           [:organizer {:CN "Malcolm"} "Malcolm"]
           "ORGANIZER;CN=MALCOLM:Malcolm"))

    (testing "Quoted parameter retains case"
      (are [input expected] (same input expected)
           [:organizer {:CN "\"Malcolm\""} "Malcolm"]
           "ORGANIZER;CN=\"Malcolm\":Malcolm"))

    (testing "Multiple values"
      (are [input expected] (same input expected)
           [:categories ["FAMILY", "FINANCE"]]
           "CATEGORIES:FAMILY,FINANCE"))

    (testing "Object structure"
      (is (= ["BEGIN:VCALENDAR"
              "BEGIN:VEVENT"
              "SUMMARY:Bastille Day Party"
              "END:VEVENT"
              "END:VCALENDAR"]

               (->> [:vcalendar [:vevent [:summary "Bastille Day Party"]]]
                    clj-ical.format/write-object with-out-str java.io.StringReader. java.io.BufferedReader. line-seq vec))))

    (testing "Dates in UTC"
      (are [input expected] (= (to-ical input) (str expected "\r\n"))
           [:dtstart (LocalDate. 2008 10 04)]
           "DTSTART:20081004"
           ;; It is worth using time/date-time for the sole reason
           ;; that, unlike the Java Joda Time, it always creates time
           ;; in UTC.
           [:dtstart (time/date-time 2008 12 24 05 20 30 0)]
           "DTSTART:20081224T052030Z"
           [:dtstart (time/date-time 2010 8 31 23 30 0 0)]
           "DTSTART:20100831T233000Z"))

    (testing "Examples from RFC 2445"
      (are [input expected] (same input expected)
           [:organizer {:CN "\"John Smith\""} "MAILTO:jsmith@host.com"]
           "ORGANIZER;CN=\"John Smith\":MAILTO:jsmith@host.com"))))



