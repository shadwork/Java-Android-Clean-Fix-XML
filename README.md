### Android strings XML fixer
Command line tools to clean and fix error in android XML strings.

Can be useful after import strings from PhraseApp service or something else.

### Using:

#### java -jar AndroidCleanFixXml.jar fileConvertWithOverwrite.xml

#### java -jar AndroidCleanFixXml.jar fileIn.xml fileOut.xml

#### java -jar AndroidCleanFixXml.jar folderNameRecursivly


For example, put AndroidCleanFixXml.jar in Android Studio Project root folder and run to processing all strings.xml inside projects:

#### java -jar AndroidCleanFixXml.jar .

If you have problems with strange %{names} inside resulted xml just open Main.java and add value from {inside} to PHRASE_STRING, PHRASE_DECIMAL or PHRASE_FLOAT. Choose right constant to get %s or %d.

##### WARNING! Tools not using temp files! Be careful before using, BUCKUP or PUSH!   
