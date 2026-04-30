package ximtool.datresource.effectroutine

import ximtool.dat.*

object EffectRoutineSerializer {

    fun serialize(config: EffectRoutine): ByteArray {
        sanityCheckConfig(config)

        var size = 0x40 // Size of header

        val initializeOffset = size
        size += config.onInitializeEffects.sumOf { it.sizeInBytes() }.padTo16()

        val effectsOffset = size
        size += config.effects.sumOf { it.sizeInBytes() }.padTo16()

        val completionOffset = size
        size += config.onCompleteEffects.sumOf { it.sizeInBytes() }.padTo16()

        val out = ByteReader(ByteArray(size))

        val header = SectionHeader(config.datId, SectionType.S07_EffectRoutine, size)
        header.write(out)

        out.position += 0x10 // Always zero

        out.write32(initializeOffset)
        out.write32(effectsOffset)
        out.write32(completionOffset)
        out.write32(config.effects.sumOf { it.delay }) // Seems unused?

        out.position = initializeOffset
        config.onInitializeEffects.forEach { it.write(out) }

        out.position = effectsOffset
        config.effects.forEach { it.write(out) }

        out.position = completionOffset
        config.onCompleteEffects.forEach { it.write(out) }

        return out.bytes
    }

    private fun sanityCheckConfig(config: EffectRoutine) {
        check(config.onInitializeEffects.lastOrNull() == OnInitializeEffects.EndEffect)

        check(config.effects.firstOrNull() is Effects.StartRoutine)
        check(config.effects.lastOrNull() is Effects.EndRoutine)

        check(config.onCompleteEffects.lastOrNull() == OnCompleteEffects.EndEffect)
    }

}