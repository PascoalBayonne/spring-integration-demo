package com.pascoal.spring.batch.listener;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class CustomSkipPolicy implements SkipPolicy, InitializingBean {

    private static final Logger logger = Logger.getLogger(CustomSkipPolicy.class.getName());

    private Long skipLimit;
    private File folderLocation;

    @Override
    public boolean shouldSkip(Throwable exception, int skipCount) throws SkipLimitExceededException {
        if (exception instanceof FileNotFoundException) {
            return false;
        } else if ((exception instanceof FlatFileParseException) && (skipCount <= skipLimit)) {
            FlatFileParseException fileParseException = (FlatFileParseException) exception;

            StringBuilder errorMessage = new StringBuilder();

            errorMessage.append("Error while processing the line number:{ ").append(fileParseException.getLineNumber()).append("}");
            errorMessage.append("\nThe line which was skipped is \n");
            errorMessage.append(fileParseException.getInput());

            logger.info(errorMessage.toString());

            int lineNumber = fileParseException.getLineNumber();
            try {
                writeTheLineSkippedIntoFile(folderLocation, lineNumber);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }
        return false;
    }

    private void writeTheLineSkippedIntoFile(File folderLocation, int lineNumberSkipped) throws IOException {
        logger.info("Writing the line skipped into a file");

        try {
            Path folder = folderLocation.toPath();
            if (Files.notExists(folder)) {
                //Of course the folder must exist {afterPropertiesSet()-> prevents this} but let me just joke a bit
                Files.createDirectory(folder);
                logger.info("folder does not exists. Creating ...");
            }

            Path fileWithSkippedLinesToBeCreated =
                    folder.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-mm-dd-hh")) + "_ERROR");//TODO problem if job runs multiple times in a day :)

            if (Files.notExists(fileWithSkippedLinesToBeCreated)) {
                Path fileWithLinesSkipped = Files.createFile(fileWithSkippedLinesToBeCreated);
                logger.info("Creating a new file ..." + fileWithLinesSkipped.toAbsolutePath());
            }


            Charset UTF_8 = StandardCharsets.UTF_8;
            String lineSkipped = String.valueOf(lineNumberSkipped + "\n");

            logger.info("Writing the number of line which was skipped into the file...");
            Files.write(fileWithSkippedLinesToBeCreated.toAbsolutePath(), lineSkipped.getBytes(UTF_8), StandardOpenOption.APPEND);

        } catch (IOException e) {
            logger.info(e.getMessage());
            logger.info("Couldn't write the lines skipped into the FILE.ERROR");
        }

    }

    public void setSkipLimit(Long skipLimit) {
        this.skipLimit = skipLimit;
    }

    public void setFolderLocation(File folderLocation) {
        this.folderLocation = folderLocation;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(folderLocation, "The Folder doesn't exists");
        Assert.notNull(skipLimit, "Skip limit is null.");
    }
}
