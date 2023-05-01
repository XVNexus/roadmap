package xveon.roadmap

object Constants {
    // The path to the config file
    const val CONFIG_FILE_PATH = "config.json"
    // The path to the folder containing scan chunk files
    const val SCAN_FOLDER_PATH = "scan/"
    // The file extension to use on scan chunk files
    const val SCAN_FILE_EXTENSION = "txt"

    // How much to multiply chunk coordinates to get block coordinates
    const val CHUNK_SIZE = 16
    // How many bits to shift to convert block to chunk coordinates
    const val CHUNK_BIT_SHIFT = 4

    // The maximum number of iterations the scanner is allowed to run for
    const val MAX_SCAN_ITERATIONS = 100000
}
