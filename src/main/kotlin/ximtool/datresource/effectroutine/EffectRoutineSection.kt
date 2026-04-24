package ximtool.datresource.effectroutine

import ximtool.dat.*
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

object EffectRoutineSection {

    fun read(data: ByteArray): EffectRoutine {
        val br = ByteReader(data)
        return read(br)
    }

    private fun read(byteReader: ByteReader): EffectRoutine {
        val startPosition = byteReader.position
        val sectionHeader = SectionHeader.read(byteReader)

        // 0x0
        byteReader.position += 0x10

        // 0x10
        val initializersOffset = byteReader.next32() + startPosition
        val effectsOffset = byteReader.next32() + startPosition
        val completionOffset = byteReader.next32() + startPosition
        val totalDelay = byteReader.next32()

        // Sec1
        byteReader.position = initializersOffset
        val initializers = parseSection(byteReader, this::parseOnInitializeEffects)

        byteReader.position = effectsOffset
        val effects = parseSection(byteReader, this::parseEffects)

        byteReader.position = completionOffset
        val completions = parseSection(byteReader, this::parseOnCompleteEffects)

        return EffectRoutine(
            datId = sectionHeader.sectionId,
            onInitializeEffects = initializers.toMutableList(),
            effects = effects.toMutableList(),
            onCompleteEffects = completions.toMutableList(),
        )
    }

    private fun <T> parseSection(byteReader: ByteReader, parser: (ByteReader, Int, Int) -> T): List<T> {
        val operation = ArrayList<T>()

        while (true) {
            val position = byteReader.position
            val (opCode, opCodeSize) = next(byteReader)
            operation += parser.invoke(byteReader, opCode, opCodeSize - 1)

            if (opCode == 0x00) { break }
            byteReader.position = position + opCodeSize * 4
        }

        return operation
    }

    private fun next(byteReader: ByteReader): OpCode {
        val opCodeConfig = byteReader.next32()
        val opCode = opCodeConfig and 0xFF
        val opCodeSize = opCodeConfig shr 8
        return OpCode(opCode, opCodeSize)
    }

    private fun parseOnInitializeEffects(byteReader: ByteReader, opCode: Int, numArgs: Int): OnInitializeEffect {
        return when (opCode) {
            0x00 -> OnInitializeEffects.EndEffect
            else -> {
                val body = byteReader.subArray(length = (numArgs - 1) * 4)
                OnInitializeEffects.NotImplementedRoutine(opCode = opCode, body = body)
            }
        }
    }

    private fun parseEffects(byteReader: ByteReader, opCode: Int, numArgs: Int) : Effect {
        val delay = byteReader.next16()
        val duration = byteReader.next16()

        return when (opCode) {
            0x00 -> {
                Effects.EndRoutine(delay = delay)
            }
            0x01 -> {
                Effects.StartRoutine(delay = delay)
            }
            0x02 -> {
                val ref = byteReader.nextDatId()
                Effects.ParticleGenRoutine(delay = delay, duration = duration, refId = ref)
            }
            0x1E -> {
                val particleGenRef = byteReader.nextDatId()
                Effects.ParticleDampenRoutine(delay = delay, duration = duration, refId = particleGenRef)
            }
            0x2D -> {
                val id = byteReader.nextDatId()
                Effects.StopGeneratorRoutine(delay = delay, duration = duration, refId = id)
            }
            0x3F -> {
                val effectId0 = byteReader.nextDatId()
                byteReader.position += 4
                val effectId1 = byteReader.nextDatId()
                Effects.TransitionParticleEffect(delay = delay, duration = duration, stopEffect = effectId0, startEffect = effectId1)
            }
            0x73 -> {
                val loopId = byteReader.nextDatId()
                Effects.StartLoopRoutine(delay = delay, refId = loopId)
            }
            0x85 -> {
                val loopId = byteReader.nextDatId()
                Effects.EndLoopRoutine(delay = delay, refId = loopId)
            }
            else -> {
                val body = byteReader.subArray(length = (numArgs - 1) * 4)
                Effects.NotImplementedRoutine(opCode = opCode, delay = delay, duration = duration, body = body)
            }
        }
    }

    private fun parseOnCompleteEffects(byteReader: ByteReader, opCode: Int, numArgs: Int): OnCompleteEffect {
        return when (opCode) {
            0x00 -> OnCompleteEffects.EndEffect
            else -> {
                val body = byteReader.subArray(length = (numArgs - 1) * 4)
                OnCompleteEffects.NotImplementedRoutine(opCode = opCode, body = body)
            }
        }
    }

}

private data class OpCode(val opCode: Int, val opCodeSize: Int)

fun Directory.getEffectRoutines(): List<EffectRoutine> {
    return getChildren(EffectRoutine::class)
}

fun Directory.getEffectRoutine(datId: DatId): EffectRoutine {
    return getChild(EffectRoutine::class, datId)
}

fun Directory.getEffectRoutineRecursive(datId: DatId): EffectRoutine {
    return getChildRecursive(EffectRoutine::class, datId)
}

operator fun <T: Effect> EffectRoutine.get(type: KClass<T>): List<T> {
    return effects.mapNotNull { type.safeCast(it) }
}

fun EffectRoutine.appendEffectRoutine(effect: Effect) {
    effects.add(index = effects.size - 1, effect)
}