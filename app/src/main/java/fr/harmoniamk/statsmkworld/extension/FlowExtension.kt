package fr.harmoniamk.statsmkworld.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Fusionne ce flow avec un autre en un flux dont les émissions sont entrelacées (merge NON
 * ordonné). Utilisé par les ViewModels stats pour combiner la chaîne de calcul dérivée des
 * sources avec le `_state` interactif interne (cf. rule 21 : attention à l'ordre avec `flowOn`).
 *
 * @param flow Le second flow à fusionner avec celui-ci.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.mergeWith(flow: Flow<T>): Flow<T> = flowOf(this, flow)
    .flattenMerge()

/**
 * Émet une valeur dans ce `MutableSharedFlow` depuis un scope donné (helper pour émettre un
 * événement one-shot hors d'une coroutine suspendante, ex. depuis un callback UI).
 *
 * @param element Valeur à émettre.
 * @param lifecycleScope Scope dans lequel lancer l'émission.
 * @return Le [Job] de la coroutine d'émission.
 */
fun <T> MutableSharedFlow<T>.emit(element: T, lifecycleScope: CoroutineScope): Job =
    lifecycleScope.launch {
        this@emit.emit(element)
    }