@Component
export default class ExTable extends Vue {
    @Prop({ type: Array, default: () => [] }) columns: [];
    @Prop({ type: Array, default: () => [] }) rows: [];

    theme = {
        color: '#2c3e50',
        //frozenRowsColor: '#2c3e50',
        frozenRowsBgColor: '#f3f3f3',
        borderColor: 'silver',
        highlightBorderColor: '#409EFF',
        font: '33px',
        // selectionBgColor: 'green',
        // highlightBgColor: '#B3B3B3'
    }

    options = {
        header: [],
        keyboardOptions: {
            moveCellOnTab: true
        }
    }

    updateSize() {
        if (this.$refs.grid) {
            this.$refs.grid.updateSize();
            // this.$refs.grid.updateScroll();
            this.$refs.grid.invalidate();
        }
    }

    @Watch('columns', { deep: true })
    onColumnsChanged() {
        let header = [];
        for (const col of this.columns) {
            header.push({ field: col.field, caption: col.title, width: col.width })
        }
        this.options.header = header;
    }

    mounted() {
        this.onColumnsChanged(); //调用一下刷新
    }

}
