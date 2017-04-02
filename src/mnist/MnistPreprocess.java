/*
 * Copyright (c) 2017.
 *
 * This file is part of Project AGI. <http://agi.io>
 *
 * Project AGI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Project AGI.  If not, see <http://www.gnu.org/licenses/>.
 */

package mnist;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * Created by Richard on 25/06/17, based on dave's code on 5/04/16
 */

public class MnistPreprocess {



    public static void main(String[] args) {

        Map<String, String> parsedArguments = new HashMap<>();

        final String GENERAL_USAGE_MESSAGE = "The purpose of this application is to convert the mnist data files (yann.lecun.com/exdb/mnist/) into a seperate PNG files ";
        String imageFilePath;
        String labelFilePath;
        String outputFolderPath;
        String isRandomised;
        String showUsage;
        String entriesCountToBeProcessed;

        List<CLISimpleParser.ArgumentEntry> inputArguments = new ArrayList<>();

        //creating input argument entities

        CLISimpleParser.ArgumentEntry imageArgument = new CLISimpleParser.ArgumentEntry("i", "image",
                "path to mnist image file", false, false, "",(arg) ->  {
                if (!isFilenameValid(arg)) {
                    throw new RuntimeException("Error! Invalid image file name. " + System.lineSeparator());
                    //return false;

                } else if(!MnistDataFile.isValidMnistFile(arg, true)){
                    throw new RuntimeException("Error! The image file seems to be invalid. Invalid magic number in mnist image file" + System.lineSeparator());
                    //return false;
                } else {
                    return true;
                }
            });


        inputArguments.add(imageArgument);



        CLISimpleParser.ArgumentEntry labelArgument = new CLISimpleParser.ArgumentEntry("l", "label",
                "path to mnist label file", false, false, "", (arg) -> {
                if (!isFilenameValid(arg)) {
                    throw new RuntimeException("Error! Invalid label file name. " + System.lineSeparator());

                } else if(!MnistDataFile.isValidMnistFile(arg, false)){
                    throw new RuntimeException("Error! The label file seems to be invalid. Invalid magic number in mnist label file" + System.lineSeparator());
                } else {
                    return true;
                }
            });
        inputArguments.add(labelArgument);


        CLISimpleParser.ArgumentEntry outputArgument  = new CLISimpleParser.ArgumentEntry("o", "output",
                "path to a folder for saving the generated outputs", false, false, "",
                (arg) ->  {
                        if (!isFilenameValid(arg)) {
                            throw new RuntimeException("Error! Invalid output folder name. " + System.lineSeparator());
                        } else {
                            return true;
                        }
                    });

        inputArguments.add(outputArgument);

        CLISimpleParser.ArgumentEntry randomisedArgument = new CLISimpleParser.ArgumentEntry("r", "randomise",
                "to indicate whether the output files' names should be randomised", true, true,
                "false", (arg) -> {
                Boolean.valueOf(arg);
                return true;

        });
        inputArguments.add(randomisedArgument);


        CLISimpleParser.ArgumentEntry numberArgument = new CLISimpleParser.ArgumentEntry("n", "number",
                "indicates how many images form the input file should be processed", true,
                false, String.valueOf(Integer.MAX_VALUE), (arg) ->  {

                Integer.valueOf(arg);
                return true;
        });

        inputArguments.add(numberArgument);


        CLISimpleParser.ArgumentEntry showUsageArgument = new CLISimpleParser.ArgumentEntry("h", "help",
                "show usage and help message", true, true, "false", (arg) -> {
                Boolean.valueOf(arg);
                return true;
        });

        inputArguments.add(showUsageArgument);

        CLISimpleParser parser = new CLISimpleParser(inputArguments);

        try {
            parsedArguments = parser.parse(args);
        }
        catch (Exception e){
            System.err.println(e.getMessage());
            return;
        }


        imageFilePath               = parsedArguments.get(imageArgument.getLongName());
        labelFilePath               = parsedArguments.get(labelArgument.getLongName());
        outputFolderPath            = parsedArguments.get(outputArgument.getLongName());
        isRandomised                = parsedArguments.get(randomisedArgument.getLongName());
        entriesCountToBeProcessed   = parsedArguments.get(numberArgument.getLongName());
        showUsage                   = parsedArguments.get(showUsageArgument.getLongName());

        boolean isValidInputArguments = true;

        try {

            isValidInputArguments &= imageArgument.validate(imageFilePath);
            isValidInputArguments &= labelArgument.validate(labelFilePath);
            isValidInputArguments &= outputArgument.validate(outputFolderPath);
            isValidInputArguments &= randomisedArgument.validate(isRandomised);
            isValidInputArguments &= showUsageArgument.validate(showUsage);

            if(!isValidInputArguments){
                System.out.println("invalid input arguments.");
                System.err.println(getUsageMessage(GENERAL_USAGE_MESSAGE, inputArguments));
            }

            if(Boolean.valueOf(showUsage)) {
                System.out.println(getUsageMessage(GENERAL_USAGE_MESSAGE, inputArguments));
                return;
            }



            preprocess(labelFilePath, imageFilePath, outputFolderPath, Integer.valueOf(entriesCountToBeProcessed),
                    Boolean.valueOf(isRandomised));


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void preprocess(String labelFile, String imageFile, String outputPath, int maxNumImages, boolean randomise){
        int randomCharacters = 6;

        MnistDataFile imagesMNIST = new MnistDataFile(labelFile, imageFile);

        if( ( maxNumImages <= 0                        ) ||
                ( maxNumImages >= imagesMNIST.bufferSize() ) ) {
            maxNumImages = imagesMNIST.bufferSize();
        }

        HashMap< String, Integer > labelCount = new HashMap<>();

        for( int i = 0; i < maxNumImages; i++ ) {

            BufferedImage image = imagesMNIST.getImage();
            String label = imagesMNIST.getLabel();

            imagesMNIST.nextImage();

            Integer count = 0;
            if( labelCount.containsKey( label ) ) {
                count = labelCount.get( label );
                count++;
            }

            labelCount.put( label, count );

            String random = "";
            if( randomise ) {
                String uuid = UUID.randomUUID().toString().replaceAll( "-", "" );
                random = "_" + uuid.substring( 0, randomCharacters );
            }

//            String outputFileName = outputPath + File.separator + random + "_" + label + "_" + labelCount.get( label ) + ".png";
            String outputFileName = outputPath + random + "_" + label + "_" + labelCount.get( label ) + ".png";
            File outputFile = new File( outputFileName ); // I removed the leading zero because there may be more needed so it becomes a malformed number

            try {
                ImageIO.write( image, "png", outputFile );
            }
            catch( IOException e ) {
                e.printStackTrace();
            }
        }


    }

    private static String getUsageMessage(String generalMessage, List<CLISimpleParser.ArgumentEntry> argumentEntries){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(generalMessage).append(System.lineSeparator());

        for (CLISimpleParser.ArgumentEntry argumentEntry : argumentEntries){
            stringBuilder.append("--").
                          append(argumentEntry.getLongName()).
                          append(", -").
                          append(argumentEntry.getShortName()).append(": ").append(System.lineSeparator()).append("\t\t").
                          append(argumentEntry.getDescription()).
                          append(System.lineSeparator()).
                          append(System.lineSeparator());
        }
        return stringBuilder.toString();
    }


    //based on http://stackoverflow.com/questions/6499609/how-to-check-whether-a-string-location-is-a-valid-saving-path-in-java
    private static boolean isFilenameValid(String fileFolderName){
        boolean result;

        File f = new File(fileFolderName);
        try {
            f.getCanonicalPath();
            result = true;
        }
        catch (IOException e) {
            result = false;
        }

        return result;
    }


}