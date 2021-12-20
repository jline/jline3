#!/usr/bin/env bash

theme=${PROFILE_NAME,,}
theme=${theme// /-}'.nanorctheme'
cp nanorctheme.template ${theme}
sed -i "s/%COLOR_01/${COLOR_01}/g" ${theme}
sed -i "s/%COLOR_02/${COLOR_02}/g" ${theme}
sed -i "s/%COLOR_03/${COLOR_03}/g" ${theme}
sed -i "s/%COLOR_04/${COLOR_04}/g" ${theme}
sed -i "s/%COLOR_05/${COLOR_05}/g" ${theme}
sed -i "s/%COLOR_06/${COLOR_06}/g" ${theme}
sed -i "s/%COLOR_07/${COLOR_07}/g" ${theme}
sed -i "s/%COLOR_08/${COLOR_08}/g" ${theme}
sed -i "s/%COLOR_09/${COLOR_09}/g" ${theme}
sed -i "s/%COLOR_10/${COLOR_10}/g" ${theme}
sed -i "s/%COLOR_11/${COLOR_11}/g" ${theme}
sed -i "s/%COLOR_12/${COLOR_12}/g" ${theme}
sed -i "s/%COLOR_13/${COLOR_13}/g" ${theme}
sed -i "s/%COLOR_14/${COLOR_14}/g" ${theme}
sed -i "s/%COLOR_15/${COLOR_15}/g" ${theme}
sed -i "s/%COLOR_16/${COLOR_16}/g" ${theme}
exit