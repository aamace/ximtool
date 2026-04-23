package ximtool.datresource.particle

import ximtool.dat.*

class ParticleGeneratorConfig(
    val datId: DatId,
    val header: ParticleGeneratorHeader,
    val generatorUpdaters: List<ParticleGeneratorUpdater>,
    val particleInitializers: List<ParticleInitializer>,
    val particleUpdaters: List<ParticleUpdater>,
    val particleExpirationHandlers: List<ParticleExpirationHandler>,
)

object ParticleGenerator {

    fun create(config: ParticleGeneratorConfig): ByteArray {
        return ParticleGeneratorCreator(config).create()
    }

}

private class ParticleGeneratorCreator(val config: ParticleGeneratorConfig) {

    private val allocationPool = HashMap<Int, Int>()

    fun create(): ByteArray {
        var size = 0x90 // Size of header

        val generatorUpdaterOffset = size
        size += config.generatorUpdaters.sumOf { it.sizeInBytes() }.padTo16()

        val initializerOffset = size
        size += config.particleInitializers.sumOf { it.sizeInBytes() }.padTo16()

        val updaterOffset = size
        size += config.particleUpdaters.sumOf { it.sizeInBytes() }.padTo16()

        val expirationHandlerOffset = size
        size += config.particleExpirationHandlers.sumOf { it.sizeInBytes() }.padTo16()

        val out = ByteReader(ByteArray(size))
        SectionHeader(config.datId, SectionType.S05_ParticleGenerator, size).write(out)

        writeGeneratorHeader(out)

        out.position = 0x80
        out.write32(generatorUpdaterOffset)
        out.write32(initializerOffset)
        out.write32(updaterOffset)
        out.write32(expirationHandlerOffset)

        writeGeneratorUpdaters(out, generatorUpdaterOffset)
        writeParticleInitializers(out, initializerOffset)
        writeParticleUpdaters(out, updaterOffset)
        writeParticleExpirationHandlers(out, expirationHandlerOffset)

        return out.bytes
    }

    private fun writeGeneratorHeader(out: ByteReader) {
        val start = out.position

        var attachField = config.header.attachType.flag
        attachField += config.header.attachedJoint0 shl 4
        attachField += config.header.attachedJoint1 shl 10
        out.write16(attachField)

        var attachScaling = 0
        attachScaling += when (config.header.actorScaleParams.scalePosition) {
            ActorScaleTarget.None -> 0x0000
            ActorScaleTarget.Source -> 0x0040
            ActorScaleTarget.Target -> 0x0080
        }
        attachScaling += when (config.header.actorScaleParams.scaleSize) {
            ActorScaleTarget.None -> 0x0000
            ActorScaleTarget.Source -> 0x0400
            ActorScaleTarget.Target -> 0x0800
        }

        out.write16(attachScaling)
        out.align0x10()

        out.writeFloat(config.header.actorScaleParams.scalePositionAmount)
        out.writeFloat(config.header.actorScaleParams.scaleSizeAmount)

        out.position = start + 0x64
        out.write16(config.header.emissionVariance)
        out.write16(config.header.framesPerEmission)
        out.write8((config.header.particlesPerEmission - 1).coerceAtLeast(0))

        var genflags = 0
        if (config.header.continuous) { genflags += 0x04 }
        out.write8(genflags)
    }

    private fun writeGeneratorUpdaters(out: ByteReader, generatorUpdaterOffset: Int) {
        out.position = generatorUpdaterOffset
        config.generatorUpdaters.forEach {
            val startPosition = out.position
            it.write(out)
            check(out.position == startPosition + it.sizeInBytes()) { "Wrote wrong number of bytes: $it" }
        }
    }

    private fun writeParticleInitializers(out: ByteReader, initializerOffset: Int) {
        val allocationPoolSize = config.particleInitializers.sumOf { it.allocationSizeInBytes() }
        var currentAllocationOffset = allocationPoolSize

        out.position = initializerOffset
        val context = InitializerContext(allocationPoolSize = allocationPoolSize)

        for (initializer in config.particleInitializers) {
            val allocationSize = initializer.allocationSizeInBytes()

            val allocationOffset = if (allocationSize > 0) {
                currentAllocationOffset -= allocationSize
                check(currentAllocationOffset >= 0) { "Illegal allocation offset $currentAllocationOffset" }
                allocationPool[initializer.opCode] = currentAllocationOffset
                currentAllocationOffset
            } else {
                0
            }

            val startPosition = out.position
            initializer.write(out, allocationOffset, context)
            check(out.position == startPosition + initializer.sizeInBytes()) { "Wrote wrong number of bytes: $initializer" }
        }
    }

    private fun writeParticleUpdaters(out: ByteReader, updaterOffset: Int) {
        out.position = updaterOffset

        for (updater in config.particleUpdaters) {
            val linked = updater.linkedInitializerOpCode()
            val allocationOffset = if (linked != null) {
                allocationPool[linked] ?: throw IllegalStateException("Updater is missing its required initializer: $linked")
            } else {
                0
            }

            val startPosition = out.position
            updater.write(out, allocationOffset)
            check(out.position == startPosition + updater.sizeInBytes()) { "Wrote wrong number of bytes: $updater" }
        }
    }

    private fun writeParticleExpirationHandlers(out: ByteReader, expirationHandlerOffset: Int) {
        out.position = expirationHandlerOffset
        config.particleExpirationHandlers.forEach {
            val startPosition = out.position
            it.write(out)
            check(out.position == startPosition + it.sizeInBytes()) { "Wrote wrong number of bytes: $it" }
        }
    }

}
