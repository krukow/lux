(ns lux.inspection.luke
  (:require [clojure.contrib.java-utils :as ju]
	    [clojure.contrib.seq-utils :as su]))

(defn- to-str-option [x]
  (if (keyword? x)
    (ju/as-str "-" x)
    x))

(defn- to-str-options [opts]
  (let [arr (make-array String (count opts))]
    (doall
     (map (fn [[i x]] (aset arr i (to-str-option x))) (su/indexed opts)))
    arr))


(defn start-luke
  "Luke [-index path_to_index] [-ro] [-force] [-mmap] [-script filename]

	-index path_to_index	open this index
	-ro	open index read-only
	-force	force unlock if the index is locked (use with caution)
	-xmlQueryParserFactory	Factory for loading custom XMLQueryParsers.
E.g.:
			org.getopt.luke.xmlQuery.CoreParserFactory (default)
			org.getopt.luke.xmlQuery.CorePlusExtensionsParserFactory
	-mmap	use MMapDirectory
	-script filename	run this script using the ScriptingPlugin.
		If an index name is specified, the index is open prior to
		starting the script. Note that you need to escape special
		characters twice - first for shell and then for JavaScript."

  ([] (org.getopt.luke.Luke/startLuke (make-array String 0)))
  ([& opts] (org.getopt.luke.Luke/startLuke (to-str-options opts))))


(defn stop-luke
  "Cleans up resources associated with Luke. Due to a design problem with Luke,
it cannot currently close the UI"
  [luke] (.actionExit luke))