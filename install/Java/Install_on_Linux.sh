#!/bin/sh
if [ -f ~/.zshrc ]
then
  SHELL_SCRIPT_PROFILE=~/.zshrc
elif [ -f ~/.bashrc ]
then
  SHELL_SCRIPT_PROFILE=~/.bashrc
else
  SHELL_SCRIPT_PROFILE=
  echo 'ERROR: Could not find rc file for installation.'
fi

# Now we need to find the absolute path of where this shell lives on the filesystem (no links or relative paths)
TARGET_FILE=$0
cd `dirname $TARGET_FILE`
TARGET_FILE=`basename $TARGET_FILE`

# Iterate down a (possible) chain of symlinks
while [ -L "$TARGET_FILE" ]
do
    TARGET_FILE=`readlink $TARGET_FILE`
    cd `dirname $TARGET_FILE`
    TARGET_FILE=`basename $TARGET_FILE`
done

# Compute the canonicalized name by finding the physical path
# for the directory we're in and appending the target file.
PHYS_DIR=`pwd -P`
FULL_PATH=$PHYS_DIR


if [ "$SHELL_SCRIPT_PROFILE" != "" ]
then
  echo 'Installing/Updating nax alias in '$SHELL_SCRIPT_PROFILE'...'
  sed -i'' -e 's/^alias nax.*//' $SHELL_SCRIPT_PROFILE
  echo 'alias nax='$FULL_PATH'/bin/nax.sh' >> $SHELL_SCRIPT_PROFILE
fi
