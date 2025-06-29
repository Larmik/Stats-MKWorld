package fr.harmoniamk.statsmkworld.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.mergeWith(flow: Flow<T>): Flow<T> = flowOf(this, flow)
    .flattenMerge()

fun <T> MutableSharedFlow<T>.emit(element: T, lifecycleScope: CoroutineScope): Job =
    lifecycleScope.launch {
        this@emit.emit(element)
    }