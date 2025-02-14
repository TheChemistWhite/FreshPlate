package com.example.freshplate.Components

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class LoadingLogoView(context: Context) : GLSurfaceView(context) {
    private val renderer: LogoRenderer

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)

        renderer = LogoRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

class LogoRenderer : GLSurfaceView.Renderer {
    private var rotationAngle = 0f
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val staticMatrix = FloatArray(16)

    private val greenColor = floatArrayOf(0.129f, 0.835f, 0.314f, 1.0f)
    private val whiteColor = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

    private val circle = Circle()
    private val clockHand = ClockHand()
    private val circularArrow = CircularArrow()
    private val sideLines = SideLines()

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        circle.init()
        clockHand.init()
        circularArrow.init()
        sideLines.init()
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Set camera position
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, -2.8f,  // Camera position
            0f, 0f, 0f,     // Look at point
            0f, 1.0f, 0f    // Up vector
        )

        // Calculate static matrix for non-rotating components
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(staticMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        // Draw static components with static matrix
        circle.draw(staticMatrix, greenColor)
        clockHand.draw(staticMatrix, whiteColor)
        sideLines.draw(staticMatrix, greenColor)

        // Create rotation matrix for circular arrow only
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationAngle, 0f, 0f, 1.0f)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        // Draw rotating circular arrow
        circularArrow.draw(mvpMatrix, greenColor)

        // Update rotation
        rotationAngle += 2.0f
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 7f)
    }
}

object GLUtils {
    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Shader compilation failed: $log")
            }
        }
    }

    fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        return GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                throw RuntimeException("Program linking failed: $log")
            }
        }
    }
}

class Circle {
    private var program: Int = 0
    private var vertexBuffer: FloatBuffer
    private val vertices = ArrayList<Float>()

    init {
        val radius = 0.7f
        val steps = 100
        val angleStep = (2 * Math.PI / steps).toFloat()

        for (i in 0..steps) {
            val angle = i * angleStep
            vertices.add((radius * cos(angle)))
            vertices.add((radius * sin(angle)))
            vertices.add(0f)
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices.toFloatArray())
        vertexBuffer.position(0)
    }

    fun init() {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """

        program = GLUtils.createProgram(vertexShaderCode, fragmentShaderCode)
    }

    fun draw(mvpMatrix: FloatArray, color: FloatArray) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glLineWidth(12.0f)
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, vertices.size / 3)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}

class ClockHand {
    private var program: Int = 0
    private var vertexBuffer: FloatBuffer
    private val vertices = floatArrayOf(
        0f, 0f, 0f,      // Center
        0f, 0.5f, 0f    // End point
    )

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)
    }

    fun init() {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """

        program = GLUtils.createProgram(vertexShaderCode, fragmentShaderCode)
    }

    fun draw(mvpMatrix: FloatArray, color: FloatArray) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glLineWidth(6.0f)  // Thicker clock hand
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}

class CircularArrow {
    private var program: Int = 0
    private var vertexBuffer: FloatBuffer
    private val vertices = ArrayList<Float>()

    init {
        val radius = 0.85f
        val startAngle = -45f
        val sweepAngle = 315f
        val numPoints = 50

        // Generate arc vertices
        for (i in 0..numPoints) {
            val angle = Math.toRadians((startAngle + (sweepAngle * i / numPoints)).toDouble())
            vertices.add((radius * cos(angle)).toFloat())
            vertices.add((radius * sin(angle)).toFloat())
            vertices.add(0f)
        }

        // Add arrow head
        val endAngle = Math.toRadians((startAngle + sweepAngle).toDouble())
        val arrowSize = 0.25f

        // Arrow tip
        vertices.add((radius * cos(endAngle)).toFloat())
        vertices.add((radius * sin(endAngle)).toFloat())
        vertices.add(0f)

        // Arrow sides
        vertices.add((radius * cos(endAngle) + arrowSize * cos(endAngle - Math.PI/6)).toFloat())
        vertices.add((radius * sin(endAngle) + arrowSize * sin(endAngle - Math.PI/6)).toFloat())
        vertices.add(0f)

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices.toFloatArray())
        vertexBuffer.position(0)
    }

    fun init() {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """

        program = GLUtils.createProgram(vertexShaderCode, fragmentShaderCode)
    }

    fun draw(mvpMatrix: FloatArray, color: FloatArray) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glLineWidth(6.0f)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertices.size / 3)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}

class SideLines {
    private var program: Int = 0
    private var vertexBuffer: FloatBuffer
    private val vertices = floatArrayOf(
        // Three horizontal lines
        -1.2f, 0.3f, 0f,   // Top line start
        -0.8f, 0.3f, 0f,   // Top line end

        -1.2f, 0.0f, 0f,   // Middle line start
        -0.8f, 0.0f, 0f,   // Middle line end

        -1.2f, -0.3f, 0f,  // Bottom line start
        -0.8f, -0.3f, 0f   // Bottom line end
    )

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)
    }

    fun init() {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """

        program = GLUtils.createProgram(vertexShaderCode, fragmentShaderCode)
    }

    fun draw(mvpMatrix: FloatArray, color: FloatArray) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glLineWidth(4.0f)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertices.size / 3)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}