import React, {useEffect, useMemo, useState} from 'react';
import {
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {LinearGradient} from 'expo-linear-gradient';
import {SafeAreaView} from 'react-native-safe-area-context';
import {finiteColors, finiteGradientColors} from './theme';

export type ReminderSettings = {id?: string; videoCount: number};
export type ReminderRecord = ReminderSettings & {id: string};

type Props = {
  dailyLimit: number;
  initialVideoCount?: number;
  onBack?: () => void;
  onClose?: () => void;
  onSave?: (settings: ReminderSettings) => void;
  onDelete?: (id: string) => void;
  savedReminders?: ReminderRecord[];
  editingReminder?: ReminderRecord;
  onEdit?: (id: string) => void;
  onAddNew?: () => void;
};

export default function AddReminder({
  dailyLimit,
  initialVideoCount = 4,
  onBack,
  onClose,
  onSave,
  onDelete,
  savedReminders = [],
  editingReminder,
  onEdit,
  onAddNew,
}: Props): React.JSX.Element {
  const safeLimit = Math.max(1, Math.min(100, Math.round(dailyLimit)));
  const maximumReminder = Math.max(1, safeLimit - 1);
  const initialCount = Math.min(maximumReminder, Math.max(1, editingReminder?.videoCount ?? initialVideoCount));
  const [videoCount, setVideoCount] = useState(initialCount);

  useEffect(() => {
    setVideoCount(Math.min(maximumReminder, Math.max(1, editingReminder?.videoCount ?? initialVideoCount)));
  }, [editingReminder, initialVideoCount, maximumReminder]);

  const remaining = Math.max(0, safeLimit - videoCount);
  const progress = `${Math.min(100, Math.max(8, (videoCount / safeLimit) * 100))}%` as `${number}%`;
  const sortedReminders = useMemo(
    () => [...savedReminders].sort((a, b) => a.videoCount - b.videoCount),
    [savedReminders],
  );

  return (
    <LinearGradient
      colors={finiteGradientColors}
      locations={[0, 0.55, 1]}
      start={{x: 0, y: 0}}
      end={{x: 0, y: 1}}
      style={styles.background}>
      <StatusBar barStyle="dark-content" />
      <SafeAreaView style={styles.safeArea} edges={['top', 'bottom']}>
        <ScrollView contentContainerStyle={styles.page} showsVerticalScrollIndicator={false}>
          <View style={styles.topBar}>
            <Pressable accessibilityRole="button" accessibilityLabel="Go back" onPress={onBack} style={styles.roundButton}>
              <Text style={styles.backArrow}>‹</Text>
            </Pressable>
            <Text style={styles.topLabel}>MINDFUL MILESTONE</Text>
            <Pressable accessibilityRole="button" accessibilityLabel="Close" onPress={onClose} style={styles.closeButton}>
              <Text style={styles.closeText}>×</Text>
            </Pressable>
          </View>

          <View style={styles.intro}>
            <View style={styles.spark}><Text style={styles.sparkText}>✦</Text></View>
            <Text style={styles.title}>{editingReminder ? 'Edit your check-in' : 'Choose a check-in'}</Text>
            <Text style={styles.description}>
              FINITE will pause the feed when you reach this video and show exactly how many you’ve watched—and how many remain.
            </Text>
          </View>

          <View style={styles.milestoneCard}>
            <Text style={styles.cardEyebrow}>REMIND ME AFTER</Text>
            <View style={styles.counterRow}>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Decrease reminder milestone"
                disabled={videoCount <= 1}
                onPress={() => setVideoCount(value => Math.max(1, value - 1))}
                style={({pressed}) => [styles.stepButton, videoCount <= 1 && styles.disabled, pressed && styles.pressed]}>
                <Text style={styles.stepText}>−</Text>
              </Pressable>

              <View style={styles.countWrap}>
                <Text style={styles.count}>{videoCount}</Text>
                <Text style={styles.countCaption}>{videoCount === 1 ? 'VIDEO' : 'VIDEOS'}</Text>
              </View>

              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Increase reminder milestone"
                disabled={videoCount >= maximumReminder}
                onPress={() => setVideoCount(value => Math.min(maximumReminder, value + 1))}
                style={({pressed}) => [styles.stepButton, videoCount >= maximumReminder && styles.disabled, pressed && styles.pressed]}>
                <Text style={styles.stepText}>+</Text>
              </Pressable>
            </View>

            <View style={styles.progressTrack}>
              <LinearGradient
                colors={['#648D95', '#72A697']}
                start={{x: 0, y: 0}}
                end={{x: 1, y: 0}}
                style={[styles.progressFill, {width: progress}]}
              />
            </View>
            <View style={styles.progressLabels}>
              <Text style={styles.progressLabel}>0 watched</Text>
              <Text style={styles.progressLabel}>{safeLimit} daily limit</Text>
            </View>

            <View style={styles.preview}>
              <View style={styles.previewNumber}><Text style={styles.previewNumberText}>{videoCount}</Text></View>
              <View style={styles.previewCopy}>
                <Text style={styles.previewTitle}>Your popup will say</Text>
                <Text style={styles.previewText}>
                  “You’ve watched {videoCount}. {remaining} {remaining === 1 ? 'video is' : 'videos are'} left today.”
                </Text>
              </View>
            </View>
          </View>

          {sortedReminders.length > 0 && (
            <View style={styles.savedCard}>
              <Text style={styles.savedHeading}>YOUR CHECK-INS</Text>
              {sortedReminders.map(item => (
                <View key={item.id} style={styles.savedRow}>
                  <Pressable accessibilityRole="button" onPress={() => onEdit?.(item.id)} style={styles.savedCopy}>
                    <View style={styles.savedBadge}><Text style={styles.savedBadgeText}>{item.videoCount}</Text></View>
                    <View>
                      <Text style={styles.savedTitle}>After video {item.videoCount}</Text>
                      <Text style={styles.savedDetail}>Once a day · tap to edit</Text>
                    </View>
                  </Pressable>
                  <Pressable accessibilityRole="button" accessibilityLabel={`Delete reminder at video ${item.videoCount}`} onPress={() => onDelete?.(item.id)} style={styles.savedDelete}>
                    <Text style={styles.savedDeleteText}>×</Text>
                  </Pressable>
                </View>
              ))}
            </View>
          )}

          <View style={styles.bottom}>
            <Pressable
              accessibilityRole="button"
              disabled={safeLimit <= 1}
              onPress={() => onSave?.({id: editingReminder?.id, videoCount})}
              style={({pressed}) => [styles.saveButton, safeLimit <= 1 && styles.disabled, pressed && styles.pressed]}>
              <Text style={styles.saveText}>{editingReminder ? 'Update check-in' : 'Set check-in at ' + videoCount}</Text>
              <Text style={styles.saveArrow}>→</Text>
            </Pressable>
            <Text style={styles.footerText}>
              Appears once per day while you scroll. No notification permission needed.
            </Text>
            {editingReminder && onAddNew && (
              <Pressable accessibilityRole="button" onPress={onAddNew} style={styles.textAction}>
                <Text style={styles.textActionLabel}>Add another check-in</Text>
              </Pressable>
            )}
            {editingReminder && onDelete && (
              <Pressable accessibilityRole="button" onPress={() => onDelete(editingReminder.id)} style={styles.textAction}>
                <Text style={styles.deleteLabel}>Delete this check-in</Text>
              </Pressable>
            )}
          </View>
        </ScrollView>
      </SafeAreaView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  background: {flex: 1},
  safeArea: {flex: 1},
  page: {flexGrow: 1, width: '100%', maxWidth: 430, alignSelf: 'center', paddingHorizontal: 20, paddingTop: 10, paddingBottom: 22},
  topBar: {height: 36, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between'},
  roundButton: {width: 34, height: 34, borderRadius: 17, backgroundColor: 'rgba(255,255,255,0.55)', alignItems: 'center', justifyContent: 'center'},
  backArrow: {color: finiteColors.text, fontSize: 27, lineHeight: 29},
  topLabel: {color: finiteColors.secondaryText, fontSize: 10, fontWeight: '700', letterSpacing: 1.5},
  closeButton: {width: 34, height: 34, alignItems: 'center', justifyContent: 'center'},
  closeText: {color: finiteColors.text, fontSize: 24, fontWeight: '300'},
  intro: {alignItems: 'center', paddingTop: 20, paddingBottom: 18},
  spark: {width: 44, height: 44, borderRadius: 22, backgroundColor: 'rgba(255,255,255,0.58)', alignItems: 'center', justifyContent: 'center', marginBottom: 12},
  sparkText: {color: '#9B7D48', fontSize: 19},
  title: {color: finiteColors.text, fontSize: 26, fontWeight: '700', letterSpacing: -0.5},
  description: {color: finiteColors.secondaryText, fontSize: 14, lineHeight: 20, textAlign: 'center', marginTop: 8, maxWidth: 340},
  milestoneCard: {backgroundColor: 'rgba(255,255,255,0.88)', borderRadius: 26, padding: 22, shadowColor: '#173743', shadowOpacity: 0.12, shadowRadius: 22, shadowOffset: {width: 0, height: 10}, elevation: 8},
  cardEyebrow: {color: finiteColors.secondaryText, fontSize: 10, fontWeight: '800', letterSpacing: 1.6, textAlign: 'center'},
  counterRow: {flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginVertical: 14},
  stepButton: {width: 48, height: 48, borderRadius: 18, backgroundColor: '#E2EFEC', borderWidth: 1, borderColor: '#C9DEDA', alignItems: 'center', justifyContent: 'center'},
  stepText: {color: '#4E7A78', fontSize: 25, fontWeight: '400', lineHeight: 27},
  countWrap: {alignItems: 'center'},
  count: {color: finiteColors.text, fontSize: 56, lineHeight: 60, fontWeight: '700', letterSpacing: -2},
  countCaption: {color: finiteColors.secondaryText, fontSize: 9, fontWeight: '800', letterSpacing: 1.8},
  progressTrack: {height: 9, borderRadius: 99, backgroundColor: '#DFE9E7', overflow: 'hidden', marginTop: 5},
  progressFill: {height: '100%', borderRadius: 99},
  progressLabels: {flexDirection: 'row', justifyContent: 'space-between', marginTop: 7},
  progressLabel: {color: '#718487', fontSize: 10},
  preview: {flexDirection: 'row', alignItems: 'center', backgroundColor: '#EDF5F2', borderRadius: 18, padding: 13, marginTop: 19},
  previewNumber: {width: 38, height: 38, borderRadius: 19, backgroundColor: '#D5E8E3', alignItems: 'center', justifyContent: 'center', marginRight: 11},
  previewNumberText: {color: '#466F70', fontSize: 17, fontWeight: '800'},
  previewCopy: {flex: 1},
  previewTitle: {color: finiteColors.text, fontSize: 11, fontWeight: '800', marginBottom: 3},
  previewText: {color: finiteColors.secondaryText, fontSize: 12, lineHeight: 17},
  savedCard: {backgroundColor: 'rgba(255,255,255,0.54)', borderRadius: 20, padding: 15, marginTop: 16},
  savedHeading: {color: finiteColors.secondaryText, fontSize: 9, fontWeight: '800', letterSpacing: 1.4, marginBottom: 6},
  savedRow: {minHeight: 56, flexDirection: 'row', alignItems: 'center', borderTopWidth: 1, borderTopColor: 'rgba(42,72,79,0.10)'},
  savedCopy: {flex: 1, flexDirection: 'row', alignItems: 'center'},
  savedBadge: {width: 34, height: 34, borderRadius: 12, backgroundColor: '#DDECE8', alignItems: 'center', justifyContent: 'center', marginRight: 10},
  savedBadgeText: {color: '#466F70', fontSize: 14, fontWeight: '800'},
  savedTitle: {color: finiteColors.text, fontSize: 13, fontWeight: '700'},
  savedDetail: {color: finiteColors.secondaryText, fontSize: 10, marginTop: 2},
  savedDelete: {width: 34, height: 34, alignItems: 'center', justifyContent: 'center'},
  savedDeleteText: {color: '#8E6260', fontSize: 22, fontWeight: '300'},
  bottom: {marginTop: 'auto', paddingTop: 20},
  saveButton: {minHeight: 54, borderRadius: 17, backgroundColor: finiteColors.primary, paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', shadowColor: '#274854', shadowOpacity: 0.18, shadowRadius: 12, shadowOffset: {width: 0, height: 7}, elevation: 4},
  saveText: {color: '#FFFFFF', fontSize: 15, fontWeight: '700'},
  saveArrow: {color: '#FFFFFF', fontSize: 19, marginLeft: 10},
  footerText: {color: finiteColors.secondaryText, fontSize: 10, lineHeight: 15, textAlign: 'center', marginTop: 10},
  textAction: {alignItems: 'center', paddingTop: 13},
  textActionLabel: {color: finiteColors.primary, fontSize: 12, fontWeight: '700'},
  deleteLabel: {color: '#8E6260', fontSize: 12, textDecorationLine: 'underline'},
  disabled: {opacity: 0.42},
  pressed: {opacity: 0.76, transform: [{scale: 0.985}]},
});
