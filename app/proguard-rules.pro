# G2-13 : R8 actif en release (minify + shrink resources).
# L'application n'utilise ni reflexion ni serialisation automatique :
# le JSON est lu/ecrit champ par champ, aucune regle de conservation
# supplementaire n'est necessaire pour l'instant.

# Conserver les noms des data classes si un crash release doit rester lisible.
-keepattributes SourceFile,LineNumberTable
