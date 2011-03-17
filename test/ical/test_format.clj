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

(ns ical.test-format
  (:use clojure.test)
  (:import
   [org.joda.time LocalDate LocalDateTime DateTime DateTimeZone DateTimeConstants DateMidnight]
   [org.joda.time.format ISODateTimeFormat])
  (:require ical.format
            clj-time.format
            [clj-time.core :as time]))

(deftest test-ical
  (letfn
      [(to-ical [input] (with-out-str (ical.format/write-object input)))
       (same [input expected] (= (to-ical input) (str expected "\r\n")))]

    (testing "Properties"
      (are [input expected] (same input expected)
           [:version "2.0"] "VERSION:2.0"))

    (testing "Long lines"
      (are [input expected-line-count]
           (= expected-line-count
              (binding [ical.format/*fold-column* 10]
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
                    ical.format/write-object with-out-str java.io.StringReader. java.io.BufferedReader. line-seq vec))))

    (testing "Examples from RFC 2445"
      (are [input expected] (same input expected)
           [:organizer {:CN "\"John Smith\""} "MAILTO:jsmith@host.com"]
           "ORGANIZER;CN=\"John Smith\":MAILTO:jsmith@host.com"))))


(deftest test-format-datetime
  (testing "Format of date time in UTC"
    (is (= "20100101T160000Z" (ical.format/datetime
                               (time/date-time 2010 01 01 16 00))))
    (is (= "20100101T000000Z" (ical.format/datetime
                               (time/date-time 2010 01 01))))))

