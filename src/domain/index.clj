(ns domain.index
  (:require [domain.dao :as dao]
	    [lux.core.indexing.indexing :as idx])
  (:import  [org.apache.lucene.document Document Field NumericField]
	     org.apache.lucene.index.IndexWriter))

(defn ^IndexWriter index-writer []
  (idx/index-writer "/tmp/index/contacts"))

(idx/defdocument Contact
  [["ID" {:index (idx/index :not-analyzed-no-norms)}]
   "Name"
   "Occupation"
   "Country"
   "City"
   "Zip code"
   ["Zip code numeric" {:field NumericField}]
   "Mobile phone"
   "Email (primary)"
   "Email (secondary)"
   "Address"
   "Last modified by"
   "Log"
   "Properties"
   "Modified date"
   "Company"
   "CompanyName"
   "Campaign mail"
   "Interests"
   "Office phone"
   "Private phone"
   "Campagnes"
   "Username"
   "Fax"
   "Initial content"
   "Comment"]
  :no-record)
	   
(defn index-all-contacts []
  (let [d (index-writer)]
    (idx/add-doc d (dao/contact-as-map 824552))
    (.close d)))
	   