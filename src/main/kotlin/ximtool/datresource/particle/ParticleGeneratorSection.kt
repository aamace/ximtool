package ximtool.datresource.particle

import ximtool.dat.*


object ParticleGeneratorSection {

    fun read(data: ByteArray): ParticleGenerator {
        return read(ByteReader(data))
    }

    private fun read(byteReader: ByteReader): ParticleGenerator {
        val start = byteReader.position

        val sectionHeader = SectionHeader.read(byteReader)
        val generatorHeader = readGeneratorHeader(byteReader)

        byteReader.position = start + 0x80
        val section1Offset = byteReader.next32()
        val section2Offset = byteReader.next32()
        val section3Offset = byteReader.next32()
        val section4Offset = byteReader.next32()

        byteReader.position = start + section1Offset
        val generatorUpdaters = parseSection(byteReader, this::parseGeneratorUpdater)

        byteReader.position = start + section2Offset
        val particleInitializers = parseSection(byteReader, this::parseParticleInitializer)

        byteReader.position = start + section3Offset
        val particleUpdaters = parseSection(byteReader, this::parseParticleUpdater)

        byteReader.position = start + section4Offset
        val particleExpirationHandlers = parseSection(byteReader, this::parseParticleExpirationHandler)

        return ParticleGenerator(
            datId = sectionHeader.sectionId,
            header = generatorHeader,
            generatorUpdaters = generatorUpdaters.toMutableList(),
            particleInitializers = particleInitializers.toMutableList(),
            particleUpdaters = particleUpdaters.toMutableList(),
            particleExpirationHandlers = particleExpirationHandlers.toMutableList(),
        )
    }

    private fun readGeneratorHeader(byteReader: ByteReader): ParticleGeneratorHeader {
        val dataStart = byteReader.position

        val attachFlags = byteReader.next16()
        val attachTypeFlag = attachFlags and 0x0F
        val attachType = AttachType.from(attachTypeFlag) ?: AttachType.None

        val attachedJoint0 = (attachFlags and 0x03F0) ushr (0x4)
        val attachedJoint1 = (attachFlags and 0xFC00) ushr (0xA)

        val additionalAttachFlags = byteReader.next16()
        val attachSourceOriented = (additionalAttachFlags and 0x0001) != 0

        val actorPositionScaleTarget = when {
            (additionalAttachFlags and 0x0040) != 0 -> ActorScaleTarget.Source
            (additionalAttachFlags and 0x0080) != 0 -> ActorScaleTarget.Target
            else -> ActorScaleTarget.None
        }

        val actorSizeScaleTarget = when {
            (additionalAttachFlags and 0x0400) != 0 -> ActorScaleTarget.Source
            (additionalAttachFlags and 0x0800) != 0 -> ActorScaleTarget.Target
            else -> ActorScaleTarget.None
        }

        byteReader.position = dataStart + 0x10
        val actorPositionScaleAmount = byteReader.nextFloat()
        val actorSizeScaleAmount = byteReader.nextFloat()

        val actorScaleParams = ActorScaleParams(
            scalePosition = actorPositionScaleTarget,
            scaleSize = actorSizeScaleTarget,
            scalePositionAmount = actorPositionScaleAmount,
            scaleSizeAmount = actorSizeScaleAmount,
        )

        byteReader.position = dataStart + 0x54
        val environmentId = byteReader.nextDatId().toNullIfZero()

        byteReader.position = dataStart + 0x64
        val emissionVariance = byteReader.next16()
        val framesPerEmission = byteReader.next16()
        val particlesPerEmission = byteReader.next8()

        val genFlags = byteReader.next8()
        val continuous = genFlags and 0x04 != 0
        val autoRun = genFlags and 0x10 != 0

        val unk = byteReader.next8()

        val moreFlags = byteReader.next8()
        val batched = (moreFlags and 0x20) != 0

        return ParticleGeneratorHeader(
            attachType = attachType,
            attachedJoint0 = attachedJoint0,
            attachedJoint1 = attachedJoint1,
            attachSourceOriented = attachSourceOriented,
            actorScaleParams = actorScaleParams,
            environmentId = environmentId,
            emissionVariance = emissionVariance,
            framesPerEmission = framesPerEmission,
            particlesPerEmission = particlesPerEmission + 1,
            continuous = continuous,
            autoRun = autoRun,
            batched = batched,
        )
    }

    private fun <T> parseSection(byteReader: ByteReader, parser: (ByteReader, Int) -> T): List<T> {
        val operation = ArrayList<T>()

        while (true) {
            val position = byteReader.position
            val (opCode, opCodeSize) = next(byteReader)
            operation += parser.invoke(byteReader, opCode)

            if (opCode == 0x00) { break }
            byteReader.position = position + opCodeSize * 4
        }

        return operation
    }

    private fun parseGeneratorUpdater(byteReader: ByteReader, opCode: Int): ParticleGeneratorUpdater {
        return when (opCode) {
            0x00 -> ParticleGeneratorUpdaters.EndMarker
            0x11 -> ParticleGeneratorUpdaters.AssociationUpdater.read(byteReader)
            else -> throw IllegalStateException("Unimplemented particle-generator updater ${opCode.toString(0x10)}")
        }
    }

    private fun parseParticleInitializer(byteReader: ByteReader, opCode: Int): ParticleInitializer {
        return when (opCode) {
            0x00 -> ParticleInitializers.EndMarker
            0x01 -> ParticleInitializers.StandardParticleSetup.read(byteReader)
            0x02 -> ParticleInitializers.VelocityInitializer(byteReader.nextVector3f())
            0x07 -> ParticleInitializers.PositionVarianceMedium.read(byteReader)
            0x08 -> ParticleInitializers.RelativeVelocitySetup(byteReader.nextFloat())
            0x09 -> ParticleInitializers.RotationInitializer(byteReader.nextVector3f())
            0x0A -> ParticleInitializers.RotationVarianceInitializer(byteReader.nextVector3f())
            0x0B -> ParticleInitializers.RotationVelocitySetup(byteReader.nextVector3f())
            0x0C -> ParticleInitializers.RotationVelocityVarianceSetup(byteReader.nextVector3f())
            0x0F -> ParticleInitializers.ScaleInitializer(byteReader.nextVector3f())
            0x11 -> ParticleInitializers.UniformScaleVariance(byteReader.nextFloat())
            0x12 -> ParticleInitializers.ScaleVelocitySetup(byteReader.nextVector3f())
            0x16 -> ParticleInitializers.ColorInitializer(byteReader.nextRGBA())
            0x19 -> ParticleInitializers.ColorTransformSetup(byteReader.next16Signed(), byteReader.next16Signed(), byteReader.next16Signed(), byteReader.next16Signed())
            0x1D -> ParticleInitializers.SpriteSheetSetup(byteReader.next32())
            0x2A -> ParticleInitializers.ColorRedKeyFrameSetup(readKeyFrameConfig(byteReader))
            0x2B -> ParticleInitializers.ColorGreenKeyFrameSetup(readKeyFrameConfig(byteReader))
            0x2C -> ParticleInitializers.ColorBlueKeyFrameSetup(readKeyFrameConfig(byteReader))
            0x2D -> ParticleInitializers.ColorAlphaKeyFrameSetup(readKeyFrameConfig(byteReader))
            0x32 -> ParticleInitializers.HazeSetup(byteReader.nextFloat(), byteReader.nextFloat())
            0x3B -> ParticleInitializers.IncrementalRotationInitializer(byteReader.nextVector3f())
            0x41 -> ParticleInitializers.RelativeVelocityVarianceSetup(byteReader.nextFloat())
            0x48 -> ParticleInitializers.ParentColorConfig
            0x4C -> ParticleInitializers.AudioRangeSetup(byteReader.nextFloat(), byteReader.nextFloat(), byteReader.nextFloat())
            0x72 -> ParticleInitializers.ProjectionBiasInitializer(byteReader.nextFloat(), byteReader.nextFloat())
            else -> throw IllegalStateException("Unimplemented particle-initializer ${opCode.toString(0x10)}")
        }
    }

    private fun parseParticleUpdater(byteReader: ByteReader, opCode: Int): ParticleUpdater {
        return when (opCode) {
            0x00 -> ParticleUpdaters.EndMarker
            0x02 -> ParticleUpdaters.PositionUpdater
            0x03 -> ParticleUpdaters.PositionVelocityAccelerator(byteReader.nextVector3f())
            0x05 -> ParticleUpdaters.RotationUpdater
            0x06 -> ParticleUpdaters.RotationVelocityAccelerator(byteReader.nextVector3f())
            0x08 -> ParticleUpdaters.ScaleUpdater
            0x09 -> ParticleUpdaters.ScaleVelocityAccelerator(byteReader.nextVector3f())
            0x0B -> ParticleUpdaters.ColorTransformApplier
            0x0D -> ParticleUpdaters.SpriteSheetUpdater
            0x0E -> ParticleUpdaters.LifeTimeUpdater
            0x18 -> ParticleUpdaters.ColorRedKeyFrameUpdater
            0x19 -> ParticleUpdaters.ColorGreenKeyFrameUpdater
            0x1A -> ParticleUpdaters.ColorBlueKeyFrameUpdater
            0x1B -> ParticleUpdaters.ColorAlphaKeyFrameUpdater
            0x2C -> ParticleUpdaters.VelocityDampener(byteReader.nextFloat(), byteReader.nextFloat())
            0x2E -> ParticleUpdaters.DistanceBasedFadeUpdater(byteReader.nextFloat(), byteReader.nextFloat())
            else -> throw IllegalStateException("Unimplemented particle-updater ${opCode.toString(0x10)}")
        }
    }

    private fun parseParticleExpirationHandler(byteReader: ByteReader, opCode: Int): ParticleExpirationHandler {
        return when(opCode) {
            0x00 -> ParticleExpirationHandlers.EndMarker
            0x05 -> ParticleExpirationHandlers.Repeat
            else -> throw IllegalStateException("Unimplemented expiration-handler ${opCode.toString(0x10)}")
        }
    }

    private fun next(byteReader: ByteReader): OpCode {
        val opCodeConfig = byteReader.next32()
        val opCode = opCodeConfig and 0xFF
        val opCodeSize = ((opCodeConfig shr 8) and 0x1F)
        return OpCode(opCode, opCodeSize)
    }

    private fun readKeyFrameConfig(byteReader: ByteReader) = ParticleInitializers.KeyFrameValueSetup.read(byteReader)

}

private data class OpCode(val opCode: Int, val opCodeSize: Int)

fun Directory.getParticleGenerator(datId: DatId): ParticleGenerator {
    return getChild(ParticleGenerator::class, datId)
}

fun Directory.getParticleGenerators(): List<ParticleGenerator> {
    return getChildren(ParticleGenerator::class)
}