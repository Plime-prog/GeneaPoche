package com.plime.geneapoche

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// v0.143 : memoire des sous-ecrans (departement ou ville choisis, source ouverte...).
// Un ecran qui quitte la composition (ouverture d'une fiche) perdait son etat ;
// au retour arriere on retrouve maintenant l'ecran tel qu'on l'avait laisse.
// Memoire de la session (reinitialisee au redemarrage de l'application).
object MemoireEcrans {
    val valeurs = mutableMapOf<String, Any?>()
}

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> rememberMemo(cle: String, initiale: T): MutableState<T> {
    val etat = remember(cle) {
        mutableStateOf(if (MemoireEcrans.valeurs.containsKey(cle)) MemoireEcrans.valeurs[cle] as T else initiale)
    }
    SideEffect { MemoireEcrans.valeurs[cle] = etat.value }
    return etat
}
