= Listener Delegate =
The myPDS Initial Creator feature is dependent on the ##PDS Common## - ##Listener## library and registers a listener delegate to perform the task of setting the initial creator and initial creation date attributes. This delegate registration is done by adding an entry to the file ##Windchill/custom/pds-listener-service.yaml ##as shown in the code snippet below. Note that the file uses YAML so ensure any changes you make are made using proper YAML syntax.
{{code language="yaml" layout="LINENUMBERS"}}
listeners:
  - id: initialCreator
    description: "Sets the Initial Creator attributes"
    events:
      - wt.vc.VersionControlServiceEvent/NEW_VERSION
    filter: typeId = ".*DefaultEPMDocument.*"
    impl: ext.pdsvision.creator.InitialCreatorDelegate
    disabled: false
    properties:
      name.iba: PDS_INITIAL_CREATOR_NAME
      name.format: name # name|fullName|initials
      date.iba: PDS_INITIAL_CREATION_DATE
      date.format: yyyy-MM-dd
{{/code}}
In the code snippet there is one collection defined by the ##listeners## keyword and it is to this collection the listener delegate entry must be added. The entry itself i a mapping of keys to values and each one of these keys are their corresponding values are described in the table below.
|=Key|=Value|=Comments
|id|initialCreator|Identifies the delegate
|description|Sets the Initial Creator attributes|A short but more human friendly way to describe the delegate
|events|wt.vc.VersionControlServiceEvent/NEW_VERSION|The Initial Creator should run when a new object is created and so the event for this is the NEW_VERSION event
|filter|typeId = ".*DefaultEPMDocument.*"|(((
The filter controls which objects are affected by the delegate. Default settings is to process all CAD objects
)))
|impl|ext.pdsvision.creator.InitialCreatorDelegate|This is the fully qualified name of the Initial Creator Delegate java class that contains the code that in turn sets the attributes
|disabled|false|If set to true the delegate will not be triggered. Useful if you want to temporarily turn off the delegate without removing it from the system
|properties|(((
 
)))|The value of the properties key is in itself a mapping where delegate specific properties can be set.
|properties[name.iba]|PDS_INITIAL_CREATOR_NAME|The IBA where the username will be stored
|properties[name.format]|name|(((
Defines the format to use.
\\name: stores the username
fullName: stores the user full name
initials: stores the user initials - //This requires the initials from the LDAP/AD to be mapped to an attribute on the WTUser type//
)))
|properties[date.iba]|PDS_INITIAL_CREATION_DATE|The IBA where the creation date will be stored
|properties[date.format]|yyyy-MM-dd|The date format to use
= Attributes =
The attributes assigned as the ##name.iba## and ##date.iba## properties in the delegate configuration must exist on the types of the objects that should be affected by this feature. By default the feature operates on CAD object and so the attributes must be added to the EPMDocument type using the Type & Attribute Manager.df
[[image:1749734476893-782.png||data-xwiki-image-style-border="true"]]
