package zhaoyun.example.composedemo.story.platform

internal fun calculateInputImeIntrusion(
    windowHeight: Float,
    inputAreaBottom: Float,
    imeBottom: Float,
    safetyMarginPx: Float,
): Float {
    if (inputAreaBottom <= 0f || inputAreaBottom >= windowHeight) return 0f

    return maxOf(
        0f,
        imeBottom - (windowHeight - inputAreaBottom) + safetyMarginPx,
    )
}
