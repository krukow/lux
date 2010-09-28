(ns domain.dao
  (:import [com.trifork.intratools.rest.dao IntratoolsDAOFactory
	                                    IntratoolsDAO]))

(def ^IntratoolsDAO dao (IntratoolsDAOFactory/createDAO))

(defn contact-as-map [^long id]
  (.findContactById dao id))
	   
	   
