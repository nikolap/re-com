(ns re-com.time
  (:require
    [reagent.core :as reagent]
    [clojure.string :as cljstring]
    [clojure.set :refer [superset?]]
    [re-com.box      :refer  [h-box gap]]
    [re-com.util :refer [pad-zero-number real-value]]))


; --- Private functions ---

;; TODO use re-com.util/deref-or-value instead of this local version
(defn deref-or-value [val-or-atom]
  (if (satisfies? IDeref val-or-atom) @val-or-atom val-or-atom))

(defn- time-int->hour-minute
  "Convert the time integer (e.g. 930) to a vector of hour and minute."
  [time-int]
  (if (nil? time-int)
    [nil nil]
    [(quot time-int 100)
     (rem time-int 100)]))

(defn- time-integer-from-vector
  "Return a time integer.
  ASSUMPTION: the vector contains 3 values which are -
    hour, ':' or '' and minutes."
  [vals]
  (assert (= (count vals) 3) (str "Application error: re-com.time/time-integer-from-vector expected a vector of 3 values. Got " vals))
  (let [hr (if (nil? (first vals)) 0 (first vals))
        mi (if (nil? (last vals)) 0 (last vals))]
  (+ (* hr 100) mi)))

(defn- int-from-string
  [s]
  (if (nil? s)
    nil
    (let [val (js/parseInt s)]
      (if (js/isNaN val)
        nil
        val))))

(defn- string->time-integer
  "Return a time integer from the passed string."
  [s]
  (let [matches (re-matches #"^(\d{0,2})()()$|^(\d{0,1})(:{0,1})(\d{0,2})$|^(\d{0,2})(:{0,1})(\d{0,2})$" s)
       vals (filter #(not (nil? %))(rest matches))]
    (time-integer-from-vector (map int-from-string vals))))

(defn display-string
  "Return a string display of the time."
  [[hour minute]]
  (if (and (nil? hour)(nil? minute))
    ""
    (str
      (if hour
        (pad-zero-number hour 2)
        "00")
      ":"
      (if minute
        (str (pad-zero-number minute 2))
        "00"))))

(defn- time-int->display-string
  "Return a string display of the time integer."
  [time-integer]
  (if (nil? time-integer)
    (display-string [nil nil])
    (display-string (time-int->hour-minute time-integer))))

;; --- Validation ---

(defn- validate-hours
  "Validate the first element of a time vector. Return true if it is valid."
  [time-integer min max]
  (let [hr (quot time-integer 100)]
    (if hr
      (and (if (nil? min) true (>= hr (quot min 100)))(if (nil? max) true (<= hr (quot max 100))))
      true)))

(defn- validate-minutes
  "Validate the second element of a time vector. Return true if it is valid."
  [time-integer]
  (let [mi (rem time-integer 100)]
    (if mi
      (< mi 60)
      true)))

(defn- validate-time-range
  "Validate the time in comparison to the min and max values. Return true if it is valid."
  [time-integer min max]
  (and (if (nil? min) true (>= time-integer min))
       (if (nil? max) true (<= time-integer max))))

(defn- validated-time-integer
  "Validate the values in the vector.
  If any are invalid replace them and the following values with nil."
  [time-integer min max]
  (let [tm-string   (time-int->display-string time-integer)
        range-str   (str (time-int->display-string min) "-" (time-int->display-string max))]
    (if-not (validate-hours time-integer min max)
      (do
        nil
        (.info js/console (str "WARNING: Time " tm-string " is outside range " range-str)))
      (if-not (validate-minutes time-integer)
        (do
          (.info js/console (str "WARNING: Minutes of " tm-string " are invalid."))
          (time-integer-from-vector [(quot time-integer 100) "" 0]))
        time-integer))))

(defn- valid-time-integer?
  "Return true if the passed time integer is valid."
  [time-integer min max]
  (if-not (validate-hours time-integer min max)
    false
    (if-not (validate-minutes time-integer)
      false
      (validate-time-range time-integer min max))))

(defn- got-focus
  "When the time input gets focus, select everything."
  [ev]
  (-> ev .-target .select))  ;; works, but requires fix for Chrome - see :on-mouse-up

(defn- validate-string
  "Return true if the passed string valdiates OK."
  [s]
  (let [matches (re-matches #"^(\d{0,2})()()$|^(\d{0,1})(:{0,1})(\d{0,2})$|^(\d{0,2})(:{0,1})(\d{0,2})$" s)
       vals (filter #(not (nil? %))(rest matches))]
    (= (count vals) 3)))  ;; Cannot do any further validation here - input must be finished first (why? because when entering 6:30, "63" is not valid)

(defn- validate-max-min
  [minimum maximum]
  (if-not (valid-time-integer? minimum nil nil)
    (throw (js/Error. (str "minimum " minimum " is not a valid time integer."))))
  (if-not (valid-time-integer? maximum nil nil)
    (throw (js/Error. (str "maximum " maximum " is not a valid time integer."))))
  (if (and minimum maximum)
    (if-not (< minimum maximum)
      (throw (js/Error. (str "maximum " maximum " is less than minimum " minimum "."))))))

(defn- time-changed
  "Triggered whenever the input field changes via key press on cut & paste."
  [ev tmp-model]
  (let [input-val (-> ev .-target .-value)
        valid? (validate-string input-val)]
    (when valid?
      (reset! tmp-model input-val))))

(defn- time-updated
  "Triggered whenever the input field loses focus.
  Re-validate been entered. Then update the model."
  [ev tmp-model min max callback]
  (let [input-val (-> ev .-target .-value)
        time-int (string->time-integer input-val)]
    (reset! tmp-model (display-string (time-int->hour-minute (validated-time-integer time-int @min @max))))
  (when callback (callback time-int))))

(defn- atom-on
  [model default]
  (reagent/atom (if model
                  (deref-or-value model)
                   default)))

(def time-api
  #{;; REQUIRED
    :model          ;; Integer - a time integer e.g. 930 for '09:30'
    ;; OPTIONAL
    :minimum        ;; Integer - a time integer - times less than this will not be allowed - default is 0.
    :maximum        ;; Integer - a time integer - times more than this will not be allowed - default is 2359.
    :on-change      ;; function - callback will be passed new result - a time integer or nil
    :disabled       ;; boolean or reagent/atom on boolean - when true, navigation is allowed but selection is disabled.
    :show-time-icon ;; boolean - if true display a clock icon to the right of the
    :style          ;;  map - optional css style information
    :hide-border    ;; boolean - hide border of the input box - default false.
    })

;; --- Components ---

(defn time-input
  "I return the markup for an input box which will accept and validate times.
  Parameters - refer time-api above."
  [& {:keys [model minimum maximum]}]
  (let [deref-model (deref-or-value model)
        tmp-model (atom-on (display-string (time-int->hour-minute deref-model)) "")
        min (atom-on minimum 0)
        max (atom-on maximum 2359)]
    (validate-max-min @min @max)                  ;; This will throw an error if the parameters are invalid
    (if-not (valid-time-integer? deref-model @min @max)
      (throw (js/Error. (str "model " deref-model " is not a valid time integer."))))
    (fn [& {:keys [on-change disabled show-time-icon hide-border style] :as args}]
        {:pre [(superset? time-api (keys args))]}
        (let [def-style {:margin-top "0px"
                         :padding-top "0px"
                         :font-size "11px"
                         :width "35px"}
              add-style (when hide-border {:border "none"})
              style (merge def-style add-style style)]
        [:span.input-append.bootstrap-timepicker
         {:style {}}
          [:input.input-small
            {:type "text"
             :disabled (if-let [dis (deref-or-value disabled)] dis false)
             :class "time-entry"
             :value @tmp-model
             :style style
             :on-focus #(got-focus %)
             :on-change #(time-changed % tmp-model)
             :on-mouse-up #(.preventDefault %)    ;; Chrome browser deselects on mouse up - prevent this from happening
             :on-blur #(time-updated % tmp-model min max on-change)}
            (when show-time-icon
              [:span.time-icon
               {:style {}}
               [:span.glyphicon.glyphicon-time]])
           ]]))))
