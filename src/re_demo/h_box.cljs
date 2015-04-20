(ns re-demo.h-box
  (:require [clojure.string  :as    string]
            [re-com.core     :refer [p h-box v-box box gap line scroller border label title button checkbox hyperlink-href slider horizontal-bar-tabs
                                     input-text input-textarea popover-anchor-wrapper popover-content-wrapper px] :refer-macros [handler-fn]]
            [re-com.box      :refer [h-box-args-desc v-box-args-desc box-args-desc gap-args-desc line-args-desc scroller-args-desc border-args-desc flex-child-style]]
            [re-com.util     :refer [px]]
            [re-demo.utils   :refer [panel-title title2 args-table github-hyperlink status-text]]
            [re-com.validate :refer [extract-arg-data string-or-hiccup? alert-type? vector-of-maps?]]
            [reagent.core    :as    reagent]
            [reagent.ratom   :refer-macros [reaction]]))


(def rounded-panel (merge (flex-child-style "1")
                          {:background-color "#fff4f4"
                           :border           "1px solid lightgray"
                           :border-radius    "4px"
                           :padding          "4px"}))

(def editor-style  {:font-size   "12px"
                    :line-height "20px"
                    :padding     "6px 8px"})

(def over-style    {:background-color "#fcc"})

(def h-box-style {}) ;{ :overflow "hidden"})

(def box-state (reagent/atom
                 {:hbox {:over?      false
                         :height     {:value "100px"     :omit? false :range [0 500]}
                         :width      {:value "450px"     :omit? false :range [0 1000]}
                         ;:size       {:value "none"      :omit? true  :type :none  :px "50px"  :ratio "3" :gsb "1 1 0"}
                         :justify    {:value :start      :omit? true}
                         :align      {:value :stretch    :omit? true}
                         :gap        {:value "4px"       :omit? false :range [0 100]}}
                  :box1 {:over?      false
                         :text       {:value "Box1"      :omit? false}
                         :size       {:value "none"      :omit? false :type :none  :px "50px"  :ratio "3" :gsb "1 1 0"}
                         :align-self {:value :stretch    :omit? true}
                         :height     {:value "50px"      :omit? true  :range [0 200]}
                         :min-width  {:value "50px"      :omit? true  :range [0 200]}
                         :max-width  {:value "50px"      :omit? true  :range [0 200]}}
                  :box2 {:over?      false
                         :text       {:value "Box2"      :omit? false}
                         :size       {:value "100px"     :omit? false :type :px    :px "100px" :ratio "2" :gsb "1 1 0"}
                         :align-self {:value :center     :omit? false}
                         :height     {:value "50px"      :omit? true  :range [0 300]}
                         :min-width  {:value "50px"      :omit? true  :range [0 200]}
                         :max-width  {:value "50px"      :omit? true  :range [0 200]}}
                  :box3 {:over?      false
                         :text       {:value "Box3"      :omit? false}
                         :size       {:value "1"         :omit? false :type :ratio :px "150px" :ratio "1" :gsb "1 1 0"}
                         :align-self {:value :stretch    :omit? true}
                         :height     {:value "50px"      :omit? true  :range [0 400]}
                         :min-width  {:value "50px"      :omit? true  :range [0 200]}
                         :max-width  {:value "50px"      :omit? true  :range [0 200]}}}))

(defn merge-named-params
  "given a hiccup vector v, and a map m containing named parameters, add the named parameters to v...TODO
      (merge-named-params [box :a 1] {:b 2 :c 3})
      ;; =>  [box :a 1 :b 2 :c 3]
  "
  [v m]
  (let [m      (remove (comp :omit? second) m)
        names  (keys m)
        values (map :value (vals m))]
    (into v (interleave names values))))

(defn make-box
  "produces something like:
     [box
        :size      \"0 1 100px\"
        :style     h-box-style
        :min-width \"200px\"
        :child [:div {:style rounded-panel} \"Box 1\"]]
  "
  [box-parameters]
  (let [over? (:over? box-parameters)]
    (-> [box :style h-box-style]
        (merge-named-params (dissoc box-parameters :over? :text))
        (conj :child)
        (conj [:div {:style (merge rounded-panel
                                   (when over? over-style))} (get-in box-parameters [:text :value])])))
  )

(defn demo
  "creates the hiccup for the real demo, with its child boxes and all"
  []
  (let [over? (:over? (:hbox @box-state))]
    (-> [h-box
         :padding "4px"
         :style (merge {:border "dashed 1px red"}
                       (when over? over-style))]
        (merge-named-params (dissoc (:hbox @box-state) :over?))
        (conj :children)
        (conj [(make-box (:box1 @box-state))
               (make-box (:box2 @box-state))
               (make-box (:box3 @box-state))]))))

(defn close-button
  [on-close]
  [button
   :label    [:i {:class "md-close"
                  :style {:font-size "20px"
                          :margin-left "8px"}}]
   :on-click (handler-fn (on-close))
   :class    "close"])

(defn px-editor
  [path]
  (let [model     (reaction (js/parseInt (get-in @box-state (conj path :value))))
        [min max] (get-in @box-state (conj path :range))]
    (fn [path on-close]
      [h-box
       :align    :center
       :children [[slider
                   :model     model
                   :min       min
                   :max       max
                   :width     "200px"
                   :on-change #(swap! box-state assoc-in (conj path :value) (px %))]
                  [close-button on-close]]])))

(defn justify-editor
  [path]
  (let [opts  [{:id :start :label ":start"}
               {:id :end :label ":end"}
               {:id :center :label ":center"}
               {:id :between :label ":between"}
               {:id :around :label ":around"}]
        model (reaction (get-in @box-state (conj path :value)))]
    (fn
      [path on-close]
      [h-box
       :align    :center
       :children [[horizontal-bar-tabs
                   :model     model
                   :tabs      opts
                   :style     editor-style
                   :on-change #(swap! box-state assoc-in (conj path :value) %)]
                  [close-button on-close]]])))

(defn align-editor
  [path]
  (let [opts  [{:id :start    :label ":start"}
               {:id :end      :label ":end"}
               {:id :center   :label ":center"}
               {:id :baseline :label ":baseline"}
               {:id :stretch  :label ":stretch"}]
        model (reaction (get-in @box-state (conj path :value)))]
    (fn
      [path on-close]
      [h-box
       :align    :center
       :children [[horizontal-bar-tabs
                   :model     model
                   :tabs      opts
                   :style     editor-style
                   :on-change #(swap! box-state assoc-in (conj path :value) %)]
                  [close-button on-close]]])))

(defn text-editor
  [path]
  (let [model (reaction (get-in @box-state (conj path :value)))]
    (fn
      [path on-close]
      [h-box
       :align    :center
       :children [[input-text
                   :model           model
                   :change-on-blur? false
                   :style           editor-style
                   :on-change       #(swap! box-state assoc-in (conj path :value) %)]
                  [close-button on-close]]])))

(defn box-size
  "Works out what to pass to :size from a map like {:value \"none\" :omit? false :type :none :px \"\100px\" :ratio \"1\" :gsb \"\"}"
  [size-spec]
  (cond (= (:type size-spec) :px)    (:px size-spec)
        (= (:type size-spec) :ratio) (:ratio size-spec)
        (= (:type size-spec) :gsb)   (:gsb size-spec)
        :else                        (name (:type size-spec))))

(defn size-editor
  [path]
  (let [opts         [{:id :inital :label "initial"}
                      {:id :auto   :label "auto"}
                      {:id :none   :label "none"}
                      {:id :px     :label "px"}
                      {:id :ratio  :label "ratio"}
                      {:id :gsb    :label "g s b"}]
        model        (reaction (get-in @box-state (conj path :type)))
        px-model     (reaction (js/parseInt (get-in @box-state (conj path :px))))
        ratio-model  (reaction (js/parseInt (get-in @box-state (conj path :ratio))))
        gsb-model    (reaction (get-in @box-state (conj path :gsb)))
        update-model (fn [path item new-model]
                       (swap! box-state assoc-in (conj path item) new-model)
                       (swap! box-state assoc-in (conj path :value) (box-size (get-in @box-state path))))
        size-status  (reagent/atom nil)]
    (fn
      [path on-close]
      [h-box
       :align    :center
       :children [[horizontal-bar-tabs
                   :model     model
                   :tabs      opts
                   :style     editor-style
                   :on-change #(do (update-model path :type %)
                                   (reset! size-status nil))]
                  (when (contains? #{:px :ratio :gsb} @model)
                    [gap :size "8px"])
                  (when (= @model :px)
                    [slider
                     :model     px-model
                     :min       0
                     :max       800
                     :width     "200px"
                     :style     editor-style
                     :on-change #(update-model path :px (px %))])
                  (when (= @model :ratio)
                    [slider
                     :model     ratio-model
                     :min       0
                     :max       10
                     :width     "200px"
                     :style     editor-style
                     :on-change #(update-model path :ratio (str %))])
                  (when (= @model :gsb)
                    [input-text
                     :model           gsb-model
                     :change-on-blur? false
                     :status          @size-status
                     :status-icon?    true
                     :status-tooltip  "Ignored - please enter 1 or 3 values"
                     :width           "200px"
                     :style           editor-style
                     :on-change       #(let [valid? (contains? #{1 3} (count (string/split (string/trim %) #"\s+")))]
                                        (if valid?
                                          (do (reset! size-status nil)
                                              (update-model path :gsb %))
                                          (reset! size-status :warning)))])
                  [close-button on-close]]])))

(defn indent-px
  [ident]
  (ident {:0  "0px"
          :1  "15px"
          :2  "25px"
          :3  "35px"}))

(defn code-row
  "A code row consists of:
    - up to three pieces of text (all optional), typically:
      1. parameter name
      2. parameter value
      3. rarely, a closing ']'
    - an editor to open"
  [active? indent text1 path text3 on-over editor]

  (let  [mouse-over-row?  (reagent/atom false)
         mouse-over-fn    (fn [val]
                            (reset! mouse-over-row? val)
                            (if on-over (on-over val))
                            nil)
         editor-open?     (reagent/atom false)
         omit?            (reaction (and (vector? path)
                                         (map? (get-in @box-state path))
                                         (get-in @box-state (conj path :omit?))))]
    (fn [active? indent text1 path text2 on-over editor]
      (let [arg-val    (if (vector? path)
                         (let [val (get-in @box-state (conj path :value))]
                           (cond
                             (nil? val)     "-"
                             (keyword? val) (str val)
                             :else          (str "\"" val "\"")))
                         (str path))
            row-active? (and @mouse-over-row? active?)
            arg-hiccup  [h-box
                         :width     "282px"
                         :style    (merge {:overflow "hidden"}
                                          (when row-active? {:background-color "#f0f0f0"
                                                             :cursor           "pointer"})
                                          (when @omit?      {:color            "#d0d0d0"}))
                         :attr     {:on-mouse-over #(mouse-over-fn true)
                                    :on-mouse-out  #(mouse-over-fn false)}
                         :children [[box
                                     :size "20px"
                                     :child (if (and row-active? (not (contains? (set path) :text)))
                                              [checkbox
                                               :model     (not @omit?)
                                               :on-change #(swap! box-state assoc-in (conj path :omit?) (not %))]
                                              [:span])]
                                    [gap :size (indent-px indent)]
                                    [box :size "100px" :child text1]
                                    [box
                                     :attr  {:on-click (handler-fn (swap! editor-open? not))}
                                     :child [:span
                                             [:span {:style (when row-active? {:color "blue"})} arg-val]
                                             text2]]]]]
        (if editor
          [popover-anchor-wrapper
           :showing? editor-open?
           :position :right-center
           :anchor   arg-hiccup
           :popover  [popover-content-wrapper
                      :showing? editor-open?
                      :position :right-center
                      :body     [editor path #(swap! editor-open? not)]]]
          arg-hiccup)))))


(defn editable-code
  "Shows the code in a way that values can be edited, allowing for an interactive demo."
  []
  (let [over-hbox  (fn [over?] (swap! box-state assoc-in [:hbox :over?] over?))
        over-box1  (fn [over?] (swap! box-state assoc-in [:box1 :over?] over?))
        over-box2  (fn [over?] (swap! box-state assoc-in [:box2 :over?] over?))
        over-box3  (fn [over?] (swap! box-state assoc-in [:box3 :over?] over?))]
    (fn []
      [v-box
       :children [[gap :size "20px"]

                  [v-box
                   :width "300px"
                   :style {:font-family      "Consolas, \"Courier New\", monospace"
                           :font-size        "12px"
                           :background-color "#f5f5f5"
                           :border           "1px solid lightgray"
                           :border-radius    "4px"
                           :padding          "8px"}
                   :children [[code-row false :0 "[h-box"        ""                   ""   over-hbox]
                              [code-row true  :1 "  :height"     [:hbox :height]      ""   over-hbox px-editor]
                              [code-row true  :1 "  :width"      [:hbox :width]       ""   over-hbox px-editor]
                              ;[code-row true  :1 "  :size"       [:hbox :size]        ""   over-hbox size-editor]
                              [code-row true  :1 "  :justify"    [:hbox :justify]     ""   over-hbox justify-editor]
                              [code-row true  :1 "  :align"      [:hbox :align]       ""   over-hbox align-editor]
                              [code-row true  :1 "  :gap"        [:hbox :gap]         ""   over-hbox px-editor]
                              [code-row false :1 "  :children"   " ["                 ""   over-hbox]

                              [code-row false :2 "[box "          ""                  ""   over-box1]
                              [code-row true  :3 "  :child"      [:box1 :text]       ""   over-box1 text-editor]
                              [code-row true  :3 "  :size"       [:box1 :size]       ""   over-box1 size-editor]
                              [code-row true  :3 "  :align-self" [:box1 :align-self] ""   over-box1 align-editor]
                              [code-row true  :3 "  :height"     [:box1 :height]     ""   over-box1 px-editor]
                              ;[code-row true  :3 "  :min-height" [:box1 :min-height] ""   over-box1 px-editor]
                              ;[code-row true  :3 "  :max-height" [:box1 :max-height] ""   over-box1 px-editor]
                              ;[code-row true  :3 "  :width"      [:box1 :width]     ""    over-box1 px-editor]
                              [code-row true  :3 "  :min-width"  [:box1 :min-width] ""   over-box1 px-editor]
                              [code-row true  :3 "  :max-width"  [:box1 :max-width] "]"  over-box1 px-editor]

                              [code-row false :2 "[box "          ""                  ""   over-box2]
                              [code-row true  :3 "  :child"      [:box2 :text]       ""   over-box2 text-editor]
                              [code-row true  :3 "  :size"       [:box2 :size]       ""   over-box2 size-editor]
                              [code-row true  :3 "  :align-self" [:box2 :align-self] ""   over-box2 align-editor]
                              [code-row true  :3 "  :height"     [:box2 :height]     ""   over-box2 px-editor]
                              [code-row true  :3 "  :min-width"  [:box2 :min-width] ""   over-box2 px-editor]
                              [code-row true  :3 "  :max-width"  [:box2 :max-width] "]"  over-box2 px-editor]

                              [code-row false :2 "[box "          ""                  ""   over-box3]
                              [code-row true  :3 "  :child"      [:box3 :text]       ""   over-box3 text-editor]
                              [code-row true  :3 "  :size"       [:box3 :size]       ""   over-box3 size-editor]
                              [code-row true  :3 "  :align-self" [:box3 :align-self] ""   over-box3 align-editor]
                              [code-row true  :3 "  :height"     [:box3 :height]     ""   over-box3 px-editor]
                              [code-row true  :3 "  :min-width"  [:box3 :min-width] ""   over-box3 px-editor]
                              [code-row true  :3 "  :max-width"  [:box3 :max-width] "]]" over-box3 px-editor]]]]])))


(defn panel
  []
  (fn
    []
    [v-box
     :size     "auto"
     :gap      "10px"
     :children [[panel-title "[h-box ... ]"
                 "src/re_com/box.cljs"
                 "src/re_demo/h_box.cljs"]

                [h-box
                 :gap      "100px"
                 :children [[v-box
                             :gap      "10px"
                             :width    "450px"
                             :children [[title2 "Notes"]
                                        [status-text "Stable"]
                                        [p "h-box is a container which lays out its  " [:code ":children"] " in a single horizontal row."]
                                        [p "The " [:span.bold "Layout"] " page (look LHS) describes the importance of " [:span.bold ":size"] ". The actual layout is a function of the " [:code ":size"] " of the container and the " [:code ":size"] " provided for each of the children."]
                                        [p "Todo: Nestability with v-box"]
                                        [args-table h-box-args-desc]]]
                            [v-box
                             :gap      "10px"
                             :width    "500px"
                             :height   "800px"
                             ;:style    {:border "dashed 1px #ddd"}
                             :children [[title2 "Demo"]
                                        [p "This is an " [:span.bold "interactive"] " demo.  Edit the \"code\" (in grey) and watch the boxes change.
                                            The red-dashed box is an h-box which contains up to three children."]
                                        [demo]
                                        [editable-code]]]]]
                [gap :size "30px"]]]))
