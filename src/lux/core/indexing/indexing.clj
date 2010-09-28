(ns lux.core.indexing.indexing
  (:require [clojure.contrib.java-utils :as ju])
  (:import [java.io File]
	   [org.apache.lucene.index IndexWriter IndexWriter$MaxFieldLength]
	   [org.apache.lucene.store Directory FSDirectory
	     MMapDirectory SimpleFSDirectory
	     NIOFSDirectory RAMDirectory]
	   [org.apache.lucene.document Document Field NumericField
	    Field$Store Field$Index
	    Field$TermVector]
	   org.apache.lucene.analysis.standard.StandardAnalyzer
	   org.apache.lucene.queryParser.QueryParser
	   org.apache.lucene.search.IndexSearcher
	   org.apache.lucene.util.Version))

(def index
     {:analyzed  Field$Index/ANALYZED
      :analyzed-no-norms Field$Index/ANALYZED_NO_NORMS
      :no Field$Index/NO
      :not-analyzed Field$Index/NOT_ANALYZED
      :not-analyzed-no-norms Field$Index/NOT_ANALYZED_NO_NORMS})
(def store
     {:no  Field$Store/NO
      :yes Field$Store/YES})

(def term-vector
     {:no Field$TermVector/NO
      :with-offsets Field$TermVector/WITH_OFFSETS
      :with-positions Field$TermVector/WITH_POSITIONS
      :with-positions-offsets Field$TermVector/WITH_POSITIONS_OFFSETS})

(def #^{:private true}
     default-version
     Version/LUCENE_30)
(def #^{:private true}
     default-analyzer
     (StandardAnalyzer. default-version))

(def default-field-options
     {:type Field
      :index (index :analyzed)
      :store (store :yes)
      :term-vector (term-vector :no)})

(defn ram-dir
  "Create a RAMDirectory."
  [] (RAMDirectory.))

(defn disk-dir
  "Create a new index in a directory on disk."
  ([path] (let [f (if (instance? File path)
		    path
		    (File. path))]
	    (FSDirectory/open f)))
  ([path type]
     (let [f (File. path)]
       (case type
	     :nio (NIOFSDirectory. f)
	     :mmap (MMapDirectory. f)
	     :simple (SimpleFSDirectory. f)
	     (throw (IllegalArgumentException. (str "type: " type
						    "not recognized")))))))

(defprotocol Dir
  (dir [this] "return a Lucene directory object corresponding to this"))

(extend-protocol Dir
  Directory
  (dir [this] this)
  String
  (dir [this] (disk-dir this))
  File
  (dir [this] (disk-dir this)))

(defn index-writer
  "Constructs an IndexWriter for the index in d,
   first creating it if it does not already exist.
   d must satisfy the Dir protocol.
   Uses Lucene IndexWriter constructor
   IndexWriter(Directory d, Analyzer a, IndexWriter.MaxFieldLength mfl)"
  ([d]
   {:pre [(satisfies? Dir d)]}
   (IndexWriter. (dir d) default-analyzer IndexWriter$MaxFieldLength/UNLIMITED))
  ([d a]
   {:pre [(satisfies? Dir d)]}
   (IndexWriter. (dir d) a IndexWriter$MaxFieldLength/UNLIMITED))
  ([d a mfl]
   {:pre [(satisfies? Dir d)]}
   (IndexWriter. (dir d) a mfl)))


(defprotocol Doc
  (document [desc data] "return a lucene Document corresponding to data"))

(declare make-document)

(extend-protocol Doc
  java.util.List 
  (document [desc data] (make-document desc data)))



(defprotocol FieldDescription
  (field-name [this] "return the field-name corresponding to this")
  (field-desc [this] "return a field descriptor corresponding to this"))

(extend-protocol FieldDescription
  String
  (field-name [this] this)
  (field-desc [this] [this default-field-options])

  clojure.lang.Keyword
  (field-name [this] this)
  (field-desc [this] [this default-field-options])

  clojure.lang.IPersistentVector
  (field-name [this] (first this))
  (field-desc [this] [(first this) (merge default-field-options (second this))]))

(defn mk-field-reduce [res f]
  (conj res (field-desc f)))

(defn to-symbol [& xs] (symbol (apply ju/as-str xs)))

(defn make-field [^String name value desc]
(swank.core/break)
  (if (= (:type desc) Field)
    (Field. name value
	    (:store desc)
	    (:index desc)
	    (:term-vector desc))
    (let [nf (NumericField. name  (:store desc) (:index desc))]
      (if (> value Integer/MAX_VALUE)
	(.setLongValue nf value)
        (.setIntValue nf value)))))

(defn make-document [desc data]
  (let [d (Document.)]
    (doseq [[name spec] desc]
      (let [field-value (get data name)]
	(.add d (make-field (ju/as-str name) field-value spec))))
    d))

(defmacro defdocument [name fields & opts]
  (let [desc (gensym "desc")
	data (gensym "data")
	opts (set opts)
	field-names (map field-name fields)]
    `(do
       ~(if-not (opts :no-record)
	  `(defrecord ~name [desc# as-doc# ~@(map to-symbol field-names)]
	     Doc
	     (document [this#] desc# as-doc#)))
       (let [~desc (reduce mk-field-reduce [] ~fields)]
	 ~(if-not (opts :no-record)
	    `(defn ~(to-symbol "make-" name)
	      [~data] (~(to-symbol name ".")
		       ~desc ~data
		       ~@(map (fn [x] `(get ~data ~x)) field-names))))
	 (defn ~(to-symbol "make-" name "-doc")
	   [data#] (document ~desc data#))
	 (def ~(to-symbol name "-fields") ~desc)
	 ~(if-not (opts :no-record)
	    ~(to-symbol name))))))

(defn add-doc
  ([index d] (.addDocument index d)))

(defn add-docs [index ds]
  (doseq [d ds]
    (add-doc index d)))