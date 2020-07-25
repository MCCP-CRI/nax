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

if [ "$SHELL_SCRIPT_PROFILE" != "" ]
then
  echo 'Installing/Updating nax alias in '$SHELL_SCRIPT_PROFILE'...'
  sed -i'' -e 's/^alias nax.*//' $SHELL_SCRIPT_PROFILE
  echo 'alias nax='$(dirname $0)'/bin/nax.sh' >> $SHELL_SCRIPT_PROFILE
fi
