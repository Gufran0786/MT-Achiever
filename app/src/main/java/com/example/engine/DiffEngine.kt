package com.example.engine

import com.example.model.DiffLineType
import com.example.model.FileDiffLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DiffEngine {

    suspend fun computeDiff(textA: String, textB: String): List<FileDiffLine> = withContext(Dispatchers.Default) {
        val linesA = textA.lines()
        val linesB = textB.lines()
        val result = mutableListOf<FileDiffLine>()

        // Simple Myers-style LCS diff algorithm
        val n = linesA.size
        val m = linesB.size
        val lcs = Array(n + 1) { IntArray(m + 1) }

        for (i in 0 until n) {
            for (j in 0 until m) {
                if (linesA[i] == linesB[j]) {
                    lcs[i + 1][j + 1] = lcs[i][j] + 1
                } else {
                    lcs[i + 1][j + 1] = maxOf(lcs[i + 1][j], lcs[i][j + 1])
                }
            }
        }

        var i = n
        var j = m
        val temp = mutableListOf<FileDiffLine>()

        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && linesA[i - 1] == linesB[j - 1] -> {
                    temp.add(
                        FileDiffLine(
                            lineNumberA = i,
                            lineNumberB = j,
                            text = linesA[i - 1],
                            type = DiffLineType.UNCHANGED
                        )
                    )
                    i--
                    j--
                }
                j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j]) -> {
                    temp.add(
                        FileDiffLine(
                            lineNumberA = null,
                            lineNumberB = j,
                            text = linesB[j - 1],
                            type = DiffLineType.ADDED
                        )
                    )
                    j--
                }
                i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j]) -> {
                    temp.add(
                        FileDiffLine(
                            lineNumberA = i,
                            lineNumberB = null,
                            text = linesA[i - 1],
                            type = DiffLineType.DELETED
                        )
                    )
                    i--
                }
            }
        }

        temp.reversed()
    }
}
