;; Copyright 2008-2013 Red Hat, Inc, and individual contributors.
;; 
;; This is free software; you can redistribute it and/or modify it
;; under the terms of the GNU Lesser General Public License as
;; published by the Free Software Foundation; either version 2.1 of
;; the License, or (at your option) any later version.
;; 
;; This software is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
;; Lesser General Public License for more details.
;; 
;; You should have received a copy of the GNU Lesser General Public
;; License along with this software; if not, write to the Free
;; Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
;; 02110-1301 USA, or see the FSF site: http://www.fsf.org.

(ns test.immutant.jobs
  (:use immutant.jobs
        immutant.jobs.internal
        [immutant.util :only [at-exit]]
        immutant.test.helpers
        clojure.test)
  (:require [immutant.registry :as registry])
  (:import org.immutant.jobs.JobSchedulizer
           org.projectodd.polyglot.jobs.BaseJob
           java.util.Date))

(defn fun [] :from-fun)

(def job-args (atom nil))

(registry/put "job-schedulizer"
              (proxy [JobSchedulizer] [nil]
                (activateScheduler [])
                (isStarted [] true)
                (createJob [& args]
                  (reset! job-args
                          (zipmap [:handler :name :cron-ex :singleton?] args))
                  nil)
                (createAtJob [& args]
                  (reset! job-args
                          (zipmap [:handler :name :start-at :end-at :every :repeat :singleton?]
                                  args))
                  nil)))

(defmacro with-noop-unschedule [& body]
  (let [f# (fn [_])]
    `(with-redefs [unschedule ~f#]
       ~@body)))

(deftest schedule-with-spec-first-works
  (with-noop-unschedule
    (schedule "name" "spec" fun)
    (is (= "spec" (:cron-ex @job-args)))
    ;; the f gets wrapped in another fn, so we have to call it to id it
    (is (= :from-fun ((:handler @job-args) nil)))))

(deftest schedule-with-spec-after-f-works
  (with-noop-unschedule
    (schedule "name" fun "spec")
    (is (= "spec" (:cron-ex @job-args)))
    ;; the f gets wrapped in another fn, so we have to call it to id it
    (is (= :from-fun ((:handler @job-args) nil)))))

(deftest schedule-with-spec-shoud-throw-if-given-any-at-opts
  (doseq [o [:at :in :every :repeat :until]]
    (is (thrown? IllegalArgumentException (schedule "name" fun "spec" o 0)))))

(deftest scheduling-at-jobs
  (with-noop-unschedule
    
    (testing "it should raise if given :at and :in"
      (is (thrown? IllegalArgumentException (schedule "name" fun :at 0 :in 0))))

    (testing "it should raise if given :until without :every"
      (is (thrown? IllegalArgumentException (schedule "name" fun :until 5))))

    (testing "it should raise if given :repeat without :every"
      (is (thrown? IllegalArgumentException (schedule "name" fun :repeat 5))))
    
    (testing "it should turn :in into a date"
      (let [d (Date. (+ 5000 (System/currentTimeMillis)))]
        (with-redefs [date (fn [_] d)]
          (schedule "name" fun :in 5000)
          (is (= d (:start-at @job-args))))))
    
    (testing "it should turn a long :at into a date"
      (let [at (System/currentTimeMillis)]
        (schedule "name" fun :at at)
        (is (= (Date. at) (:start-at @job-args)))))

    (testing "it should pass through a Date :at"
      (let [at (Date.)]
        (schedule "name" fun :at at)
        (is (= at (:start-at @job-args)))))

    (testing "it should turn a long :until into a date"
      (let [until (System/currentTimeMillis)]
        (schedule "name" fun :until until :every 5)
        (is (= (Date. until) (:end-at @job-args)))))

    (testing "it should pass through a Date :until"
      (let [until (Date.)]
        (schedule "name" fun :until until :every 5)
        (is (= until (:end-at @job-args)))))

    (testing "it should pass through a non-nil :every"
      (schedule "name" fun :every 5)
      (is (= 5 (:every @job-args))))
    
    (testing "it should pass through a non-nil :repeat"
      (schedule "name" fun :repeat 5 :every 5)
      (is (= 5 (:repeat @job-args))))
    
    (testing "it should treat a nil :every as 0"
      (schedule "name" fun :every nil)
      (is (= 0 (:every @job-args))))
    
    (testing "it should treat a nil :repeat as 0"
      (schedule "name" fun :repeat nil)
      (is (= 0 (:repeat @job-args))))

    (testing "it should allow no opts"
      (schedule "name" fun)
      (is (= "name" (:name @job-args))))))
