;; Copyright 2008-2012 Red Hat, Inc, and individual contributors.
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

(ns immutant.try
  (:require [clojure.tools.logging :as log]))

(defn- worked? [attempt]
  (try
    (if (symbol? attempt)
      (eval (eval attempt))
      (eval attempt))
    true
    (catch Throwable e
      (log/warn (.getMessage e))
      false)))

(defmacro try-defn [attempt name & rest]
  (let [fname (str name)]
    (if (worked? attempt)
      `(defn ~name ~@rest)
      `(defn ~name [& args#]
         (log/warn (str "The function, " ~fname ", is limited when run outside of Immutant"))
         nil))))

(defmacro try-def [attempt name & rest]
  (if (worked? attempt)
    `(def ~name ~@rest)
    `(def ~name nil)))