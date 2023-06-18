import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*
import javax.swing.border.EmptyBorder

class EncryptDecryptApp {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater { EncryptDecryptApp() }
        }
    }

    private val frame: JFrame = JFrame("AES Encrypt/Decrypt Tool")
    private val tabbedPane: JTabbedPane = JTabbedPane()
    private val encryptionService = EncryptionService()

    init {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        tabbedPane.add("Encrypt Text", createTextTab("Encrypt", encryptionService::encryptText))
        tabbedPane.add("Decrypt Text", createTextTab("Decrypt", encryptionService::decryptText))
//        tabbedPane.add("Encrypt File", createFileTab("Encrypt", encryptionService::encryptBytes))
//        tabbedPane.add("Decrypt File", createFileTab("Decrypt", encryptionService::decryptBytes))

        frame.contentPane.add(tabbedPane, BorderLayout.CENTER)
        frame.setSize(800, 600)
        frame.isVisible = true
    }

    private fun createTextTab(tabName: String, function: (String, String) -> Result<String>): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = EmptyBorder(10, 40, 20, 40)
        }

        val textAreaInput = JTextArea(10, 30).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            lineWrap = true
            wrapStyleWord = true
        }

        val passphraseField = JPasswordField().apply {
            val fieldSize = Dimension(Integer.MAX_VALUE, preferredSize.height)
            maximumSize = fieldSize
            minimumSize = fieldSize
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val textAreaOutput = JTextArea(10, 30).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            lineWrap = true
            wrapStyleWord = true
            isEditable = false
        }

        val button = JButton(tabName).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                val inputText = textAreaInput.text.trim()
                val passphrase = String(passphraseField.password)
                val result = function(inputText, passphrase)
                if (result.isSuccess) {
                    textAreaOutput.foreground = Color.BLACK
                    textAreaOutput.text = result.getOrNull()
                } else {
                    textAreaOutput.foreground = Color.RED
                    val exception = result.exceptionOrNull()
                    val sw = StringWriter()
                    exception?.printStackTrace(PrintWriter(sw))
                    textAreaOutput.text = "${exception?.message}\n$sw"
                }
            }
        }

        with(panel) {
            add(Box.createVerticalStrut(10))
            add(JLabel("Input Text:"))
            add(Box.createVerticalStrut(5))
            add(JScrollPane(textAreaInput))
            add(Box.createVerticalStrut(10))
            add(JLabel("Passphrase:"))
            add(Box.createVerticalStrut(5))
            add(passphraseField)
            add(Box.createVerticalStrut(10))
            add(button)
            add(Box.createVerticalStrut(10))
            add(JLabel("Output Text:"))
            add(Box.createVerticalStrut(5))
            add(JScrollPane(textAreaOutput))
            add(Box.createVerticalGlue())
        }

        return panel
    }

    private fun createFileTab(tabName: String, function: (ByteArray, String) -> Result<ByteArray>): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val filePathField = JTextField()
        val passphraseField = JPasswordField()
        val filePathFieldSize = Dimension(Integer.MAX_VALUE, filePathField.preferredSize.height)
        filePathField.maximumSize = filePathFieldSize
        filePathField.minimumSize = filePathFieldSize
        filePathField.alignmentX = Component.LEFT_ALIGNMENT
        passphraseField.alignmentX = Component.LEFT_ALIGNMENT

        val browseButton = JButton("Browse")
        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                filePathField.text = fileChooser.selectedFile.absolutePath
            }
        }

        val processButton = JButton("$tabName File")
        processButton.addActionListener {
            val filePath = filePathField.text.trim()
            val passphrase = String(passphraseField.password)
            val file = File(filePath)
            val bytes = Files.readAllBytes(file.toPath())
            val result = function(bytes, passphrase)
            result.onSuccess { processedBytes ->
                val outputFilePath = if (tabName == "Encrypt") "$filePath.enc" else "$filePath.dec"
                Files.write(Path.of(outputFilePath), processedBytes)
                JOptionPane.showMessageDialog(frame, "File processed successfully.\nOutput file: $outputFilePath")
            }.onFailure { exception ->
                JOptionPane.showMessageDialog(frame, "An error occurred: ${exception.localizedMessage}", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }

        panel.border = EmptyBorder(10, 40, 20, 40)
        panel.add(Box.createVerticalStrut(10))
        panel.add(JLabel("File Path:"))
        panel.add(Box.createVerticalStrut(5))
        panel.add(filePathField)
        panel.add(Box.createVerticalStrut(10))
        panel.add(browseButton)
        panel.add(Box.createVerticalStrut(10))
        panel.add(JLabel("Passphrase:"))
        panel.add(Box.createVerticalStrut(5))
        panel.add(passphraseField)
        panel.add(Box.createVerticalStrut(10))
        panel.add(processButton)
        panel.add(Box.createVerticalGlue())

        return panel
    }

}